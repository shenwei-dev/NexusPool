package com.family.chat.ai;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 工具注册表 (AiToolRegistry)。
 *
 * <p>启动时由 Spring 自动注入所有 {@link AiTool} 实现, 构建 name -> tool 映射,
 * 并预生成 OpenAI tools 声明 JSON (每轮对话直接复用, 零重复构建开销)。
 *
 * @author ShenWei
 */
@Slf4j
@Component
public class AiToolRegistry {

    private final Map<String, AiTool> tools = new LinkedHashMap<>();

    /** 预生成的 OpenAI tools 声明, 不可变缓存 */
    private final JSONArray toolsDeclaration = new JSONArray();

    public AiToolRegistry(List<AiTool> toolBeans) {
        for (AiTool tool : toolBeans) {
            AiTool previous = tools.put(tool.name(), tool);
            if (previous != null) {
                throw new IllegalStateException("Duplicate AI tool name: " + tool.name());
            }

            JSONObject function = new JSONObject();
            function.put("name", tool.name());
            function.put("description", tool.description());
            function.put("parameters", tool.parametersSchema());

            JSONObject wrapper = new JSONObject();
            wrapper.put("type", "function");
            wrapper.put("function", function);
            toolsDeclaration.add(wrapper);
        }
        log.info("AI tool registry initialized. [toolCount={}, tools={}]", tools.size(), tools.keySet());
    }

    /** OpenAI tools 声明 (每轮对话复用同一实例) */
    public JSONArray toOpenAiTools() {
        return toolsDeclaration;
    }

    /**
     * 按名称分发执行工具。
     *
     * @throws IllegalArgumentException 大模型幻觉出不存在的工具名时抛出, 由编排层转成 tool 结果反哺
     */
    public String execute(String name, Long userId, JSONObject args) {
        AiTool tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }
        return tool.execute(userId, args != null ? args : new JSONObject());
    }
}
