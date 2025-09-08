package com.rem.vendingmachine.model;

import java.time.LocalDateTime;
import java.util.List;

public class VendingMachine {

    private int id;                   // 售货机ID
    private String machineCode;       // 售货机编号
    private int status;               // 状态：1-在线，0-离线，2-需要维护
    private Double temperature;       // 当前温度
    private String locationDesc;      // 位置信息
    private LocalDateTime activationTime; // 激活时间
    private LocalDateTime updateTime;     // 更新时间

    private List<VendingMachineProduct> products;

    public VendingMachine() {}

    public VendingMachine(int id, String machineCode, int status, Double temperature, String locationDesc, LocalDateTime activationTime) {
        this.id = id;
        this.machineCode = machineCode;
        this.status = status;
        this.temperature = temperature;
        this.locationDesc = locationDesc;
        this.activationTime = activationTime;
    }

    public void applyCommand(String command) {
        // 模拟命令执行
        if ("CHANGE_TEMPERATURE".equals(command)) {
            this.temperature += 1; // 假设模拟温度增加
            System.out.println("温度已改变，当前温度：" + this.temperature);
        } else if ("DISPENSE_PRODUCT".equals(command)) {
            System.out.println("模拟售货商品出货！");
        } else {
            System.out.println("未知命令：" + command);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMachineCode() {
        return machineCode;
    }

    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public String getLocationDesc() {
        return locationDesc;
    }

    public void setLocationDesc(String locationDesc) {
        this.locationDesc = locationDesc;
    }

    public LocalDateTime getActivationTime() {
        return activationTime;
    }

    public void setActivationTime(LocalDateTime activationTime) {
        this.activationTime = activationTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "VendingMachine{" +
                "id=" + id +
                ", machineCode='" + machineCode + '\'' +
                ", status=" + status +
                ", temperature=" + temperature +
                ", locationDesc='" + locationDesc + '\'' +
                ", activationTime=" + activationTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
