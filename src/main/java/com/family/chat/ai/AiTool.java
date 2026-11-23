package com.family.chat.ai;

import com.alibaba.fastjson.JSONObject;

/**
 * AI Agent 工具 SPI 接口 (对标 OpenAI Function Calling / 通义工具协议)。
 *
 * <p>每个实现类即一个可被大模型调用的 IM 能力单元。新增 IM 能力只需:
 * 实现本接口 + 标注 @Component, 注册表自动收集, 无需改动编排逻辑。
 *
 * <p>安全约束: execute 只接收显式传入的 userId (来自 WS 会话身份),
 * 禁止依赖 ThreadLocal 上下文 (Agent 运行在异步线程池)。
 *
 * @author ShenWei
 */
public interface AiTool {

    /** 工具唯一名称, 建议使用 snake_case, 将直接暴露给大模型 */
    String name();

    /** 工具用途说明, 大模型依据它决定何时调用, 务必写清适用场景与边界 */
    String description();

    /** 入参 JSON Schema (OpenAI function parameters 格式), 无参返回空 object schema */
    JSONObject parametersSchema();

    /**
     * 执行工具。
     *
     * @param userId 当前对话用户 ID (权限边界: 只能操作该用户自己的数据)
     * @param args   大模型生成的入参 (可能缺字段, 需自行校验)
     * @return 回传给大模型的结果文本 (建议紧凑 JSON, 控制 token 消耗)
     */
    String execute(Long userId, JSONObject args);
}
