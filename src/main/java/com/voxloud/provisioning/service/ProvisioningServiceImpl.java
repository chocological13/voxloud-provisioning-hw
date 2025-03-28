package com.voxloud.provisioning.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voxloud.provisioning.entity.Device;
import com.voxloud.provisioning.exception.DeviceNotFoundException;
import com.voxloud.provisioning.repository.DeviceRepository;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Data
public class ProvisioningServiceImpl implements ProvisioningService {

    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;

    @Value("${provisioning.domain}")
    private String domain;

    @Value("${provisioning.port}")
    private String port;

    @Value("${provisioning.codecs}")
    private String codecs;


    @Override
    public String getProvisioningFile(String macAddress) throws DeviceNotFoundException {
        Device device = deviceRepository.findByMacAddress(macAddress)
            .orElseThrow(() -> new DeviceNotFoundException("Device not found"));


        return device.getModel() == Device.DeviceModel.DESK
            ? generateDeskConfig(device)
            : generateConferenceConfig(device);
    }

    private String generateDeskConfig(Device device) {
        Properties properties = new Properties();
        properties.setProperty("username", device.getUsername());
        properties.setProperty("password", device.getPassword());
        properties.setProperty("domain", domain);
        properties.setProperty("port", port);
        properties.setProperty("codecs", codecs);

        applyOverrideFragment(device, properties);

        // Convert to string
        StringBuilder config = new StringBuilder();
        properties.forEach((key, value) -> config.append(key).append("=").append(value).append("\n"));
        return config.toString().trim();
    }

    private String generateConferenceConfig(Device device) throws DeviceNotFoundException {
        Map<String, Object> config = new HashMap<>();
        config.put("username", device.getUsername());
        config.put("password", device.getPassword());
        config.put("domain", domain);
        config.put("port", port);
        config.put("codecs", codecs.split(","));

        applyOverrideFragment(device, config);

        // convert to JSON
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
        } catch (IOException e) {
            throw new DeviceNotFoundException("Error generating configuration");
        }
    }

    private void applyOverrideFragment(Device device, Properties properties) {
        if (device.getOverrideFragment() != null && !device.getOverrideFragment().isEmpty()) {
            Properties overrideProperties = new Properties();

            try {
                overrideProperties.load(new StringReader(device.getOverrideFragment()));
                overrideProperties.forEach((key, value) -> properties.setProperty(key.toString(), value.toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void applyOverrideFragment(Device device, Map<String, Object> config) {
        if (device.getOverrideFragment() != null && !device.getOverrideFragment().isEmpty()) {
            try {
                Map<String, Object> overrideConfig = objectMapper.readValue(device.getOverrideFragment(), Map.class);
                config.putAll(overrideConfig);
            } catch (IOException e) {
                // Try property-style parsing for desk devices
                Properties properties = new Properties();
                try {
                    properties.load(new StringReader(device.getOverrideFragment()));
                    properties.forEach((key, value) -> config.put(key.toString(), value));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

}
