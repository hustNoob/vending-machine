package com.rem.vendingmachine.service;

import com.rem.vendingmachine.model.VendingMachine;

import java.util.List;

public interface VendingMachineService {

    boolean addVendingMachine(VendingMachine vendingMachine);

    boolean updateVendingMachine(VendingMachine vendingMachine);

    boolean deleteVendingMachineById(int id);

    List<VendingMachine> getAllVendingMachines();

    VendingMachine getVendingMachineById(int id);

    List<VendingMachine> getVendingMachinesByStatus(int status);
}
