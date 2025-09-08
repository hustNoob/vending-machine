package com.rem.vendingmachine.service;

import com.rem.vendingmachine.dao.VendingMachineMapper;
import com.rem.vendingmachine.model.VendingMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VendingMachineServiceImpl implements VendingMachineService {

    @Autowired
    private VendingMachineMapper vendingMachineMapper;

    @Override
    public boolean addVendingMachine(VendingMachine vendingMachine) {
        return vendingMachineMapper.insertVendingMachine(vendingMachine)>0;
    }

    @Override
    public boolean updateVendingMachine(VendingMachine vendingMachine) {
        return vendingMachineMapper.updateVendingMachine(vendingMachine)>0;
    }

    @Override
    public boolean deleteVendingMachineById(int id) {
        return vendingMachineMapper.deleteVendingMachineById(id)>0;
    }

    @Override
    public List<VendingMachine> getAllVendingMachines() {
        return vendingMachineMapper.selectAllVendingMachines();
    }

    @Override
    public VendingMachine getVendingMachineById(int id) {
        return vendingMachineMapper.selectVendingMachineById(id);
    }

    @Override
    public List<VendingMachine> getVendingMachinesByStatus(int status) {
        return vendingMachineMapper.selectVendingMachinesByStatus(status);
    }
}
