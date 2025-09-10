package com.rem.vendingmachine.service;

import com.rem.vendingmachine.model.VendingMachine;

import java.time.LocalDateTime;
import java.util.List;

public interface VendingMachineService {

    boolean addVendingMachine(VendingMachine vendingMachine);

    boolean updateVendingMachine(VendingMachine vendingMachine);

    boolean deleteVendingMachineById(int id);

    List<VendingMachine> getAllVendingMachines();

    VendingMachine getVendingMachineById(int id);

    List<VendingMachine> getVendingMachinesByStatus(int status);

    //for mqtt
    void updateLastUpdateTime(int machineId, LocalDateTime lastUpdateTime);

    void updateVendingMachineStatus(int machineId, double temperature, int status);

    List<Integer> geAllVendingMachineId();
}
