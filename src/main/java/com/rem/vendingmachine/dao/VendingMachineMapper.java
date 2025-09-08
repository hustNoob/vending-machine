package com.rem.vendingmachine.dao;

import com.rem.vendingmachine.model.VendingMachine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VendingMachineMapper {

    int insertVendingMachine(VendingMachine vendingMachine);

    int updateVendingMachine(VendingMachine vendingMachine);

    int deleteVendingMachineById(@Param("id") int id);

    List<VendingMachine> selectAllVendingMachines();

    VendingMachine selectVendingMachineById(@Param("id") int id);

    List<VendingMachine> selectVendingMachinesByStatus(@Param("status") int status);   // 根据状态查询售货机列表

    // 更新售货机的心跳时间（lastUpdateTime）
    int updateLastUpdateTime(@Param("id") int id, @Param("lastUpdateTime") LocalDateTime lastUpdateTime);
}