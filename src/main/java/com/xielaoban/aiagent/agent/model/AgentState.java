package com.xielaoban.aiagent.agent.model;


/**
 * 代理执行状态的枚举类
 */
public enum AgentState {
    /**
     * 空闲状态
     */
    IDLE,
    /**
     * 执行中状态
     */
    RUNNING,
    /**
     * 错误状态
     */
    ERROR,
    /**
     * 完成状态
     */
    FINISHED
}
