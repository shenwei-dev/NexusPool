package com.family.chat.ai;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.family.chat.entity.Message;
import com.family.chat.mapper.MessageMapper;
import com.family.chat.metrics.ChatMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AI Agent 编排服务 (AiAgentService)。
 *
 * <p>对标阿里通义/钉钉 AI 助理架构: Function Calling + ReAct 多轮循环。
 * 流程: 注入系统人设与历史记忆 -> 循环 [模型决策 -> 执行工具 -> 结果反哺]
 * -> 直至模型给出最终自然语言回答或达到迭代上限。
 *
 * <p>安全护栏:
 * <ul>
 *   <li>MAX_ITERATIONS 上限, 防止模型死循环反复调用工具</li>
 *   <li>userId 由服务端从 WS 会话身份透传, 工具层禁止跨用户操作</li>
 *   <li>工具异常不中断对话, 转成文本结果反哺模型自行降级</li>
 * </ul>
 *
 * @author ShenWei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentService {

    /** AI 助手固定账号 */
    public static final Long AI_ASSISTANT_ID = 99999L;

    /** ReAct 循环最大迭代次数, 防止模型陷入工具调用死循环 */
    private static final int MAX_ITERATIONS = 5;

    /** 注入上下文的历史对话条数 (短记忆) */
    private static final int HISTORY_LIMIT = 10;

    private static final String FALLBACK_REPLY = "智能管家开小差了，请稍后再试。";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MessageMapper messageMapper;

    private final AiToolRegistry toolRegistry;

    private final LlmGateway llmGateway;

    private final ChatMetrics chatMetrics;

    /**
     * 执行一次完整的 Agent 对话决策。
     *
     * @param userId     当前用户 ID (权限边界)
     * @param userPrompt 用户本轮输入
     * @return 最终回复文本, 永不为 null
     */
    public String chat(Long userId, String userPrompt) {
        long start = System.currentTimeMillis();
        JSONArray messages = buildContext(userId, userPrompt);
        JSONArray tools = toolRegistry.toOpenAiTools();

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            JSONObject reply = llmGateway.chat(messages, tools);
            if (reply == null) {
                break;
            }

            JSONArray toolCalls = reply.getJSONArray("tool_calls");
            if (toolCalls == null || toolCalls.isEmpty()) {
                String content = reply.getString("content");
                log.info("Agent reached final answer. [userId={}, iterations={}, rt={}ms]",
                        userId, iteration + 1, System.currentTimeMillis() - start);
                return content != null ? content : FALLBACK_REPLY;
            }

            // Preserve the raw assistant message (with tool_calls & null content) verbatim,
            // otherwise the gateway rejects the follow-up request with 400
            messages.add(reply);
            executeToolCalls(userId, messages, toolCalls);
        }

        log.warn("Agent loop exhausted max iterations or gateway unavailable. [userId={}]", userId);
        return FALLBACK_REPLY;
    }

    /**
     * 构建对话上下文: 系统人设 -> 短期历史记忆 -> 本轮输入。
     */
    private JSONArray buildContext(Long userId, String userPrompt) {
        JSONArray messages = new JSONArray();

        JSONObject systemRole = new JSONObject();
        systemRole.put("role", "system");
        systemRole.put("content", buildSystemPrompt(userId));
        messages.add(systemRole);

        List<Message> historyList = messageMapper.selectRecentHistory(userId, AI_ASSISTANT_ID, HISTORY_LIMIT);
        for (Message history : historyList) {
            // Only feed plain-text turns into context; image/location payloads are noise for the LLM
            if (history.getMessageType() != null && history.getMessageType() != 0) {
                continue;
            }
            JSONObject historyMsg = new JSONObject();
            historyMsg.put("role", history.getFromUserId().equals(userId) ? "user" : "assistant");
            historyMsg.put("content", history.getContent());
            messages.add(historyMsg);
        }

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);
        return messages;
    }

    private String buildSystemPrompt(Long userId) {
        return "你是 FamilyChat 家庭聊天应用内置的智能助手「小管家」。\n"
                + "[身份与边界]\n"
                + "- 当前与你对话的用户 ID 是 " + userId + "，你只能查询和操作该用户本人的数据。\n"
                + "- 当前时间: " + LocalDateTime.now().format(TIME_FORMATTER) + "\n"
                + "[行为准则]\n"
                + "1. 涉及好友、会话、聊天记录、好友申请等信息时, 必须调用工具获取真实数据, 严禁凭空编造。\n"
                + "2. 一次回答允许连续调用多个工具; 信息不足时继续调用, 直到能给出完整答复。\n"
                + "3. 写操作(代发消息、代发好友申请、接受好友申请、会话置顶)仅在用户明确表达意图时执行, 不得自作主张。\n"
                + "4. 工具返回的 userId/friendId/requestId 是后续工具调用的关键参数, 请准确使用。\n"
                + "5. 回复口语化、简洁、温暖, 适合手机聊天界面展示; 不得暴露工具名称、参数结构等内部实现。\n"
                + "6. 工具调用失败时用通俗语言向用户说明, 不要输出错误堆栈。";
    }

    /**
     * 执行模型本轮请求的全部工具调用, 结果以 tool 角色消息反哺上下文。
     */
    private void executeToolCalls(Long userId, JSONArray messages, JSONArray toolCalls) {
        for (int i = 0; i < toolCalls.size(); i++) {
            JSONObject toolCall = toolCalls.getJSONObject(i);
            String callId = toolCall.getString("id");
            JSONObject function = toolCall.getJSONObject("function");
            String toolName = function.getString("name");
            String argumentsRaw = function.getString("arguments");

            long toolStart = System.currentTimeMillis();
            String toolResult;
            String result = "success";
            try {
                JSONObject args = (argumentsRaw == null || argumentsRaw.isBlank())
                        ? new JSONObject()
                        : JSONObject.parseObject(argumentsRaw);
                toolResult = toolRegistry.execute(toolName, userId, args);
                log.info("Agent tool executed. [userId={}, tool={}, rt={}ms]",
                        userId, toolName, System.currentTimeMillis() - toolStart);
            } catch (Exception e) {
                result = "fail";
                log.error("Agent tool execution failed. [userId={}, tool={}]", userId, toolName, e);
                toolResult = "工具调用失败: " + e.getMessage();
            } finally {
                chatMetrics.recordToolCall(toolName, result, System.currentTimeMillis() - toolStart);
            }

            JSONObject toolMessage = new JSONObject();
            toolMessage.put("role", "tool");
            toolMessage.put("tool_call_id", callId);
            toolMessage.put("name", toolName);
            toolMessage.put("content", toolResult);
            messages.add(toolMessage);
        }
    }
}
