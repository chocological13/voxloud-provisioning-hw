package com.voxloud.provisioning.service;

import com.voxloud.provisioning.exception.DeviceNotFoundException;

public interface ProvisioningService {

    String getProvisioningFile(String macAddress) throws DeviceNotFoundException;
}
