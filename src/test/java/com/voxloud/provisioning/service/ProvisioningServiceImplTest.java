package com.voxloud.provisioning.service;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voxloud.provisioning.entity.Device;
import com.voxloud.provisioning.exception.DeviceNotFoundException;
import com.voxloud.provisioning.repository.DeviceRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;


public class ProvisioningServiceImplTest {

  private ProvisioningServiceImpl provisioningService;

  @Mock
  private DeviceRepository deviceRepository;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    objectMapper = new ObjectMapper();
    provisioningService = new ProvisioningServiceImpl(deviceRepository, objectMapper);

    // default properties
    ReflectionTestUtils.setField(provisioningService, "domain", "sip.voxloud.com");
    ReflectionTestUtils.setField(provisioningService, "port", "5060");
    ReflectionTestUtils.setField(provisioningService, "codecs", "G711,G729,OPUS");
  }

  @Test
  void testGenerateConfigurationForDeskDevice() throws DeviceNotFoundException {
    // Arrange
    Device device = new Device();
    device.setMacAddress("aa-bb-cc-dd-ee-ff");
    device.setModel(Device.DeviceModel.DESK);
    device.setUsername("john");
    device.setPassword("doe");

    when(deviceRepository.findByMacAddress("aa-bb-cc-dd-ee-ff")).thenReturn(Optional.of(device));

    // Act
    String configuration = provisioningService.getProvisioningFile("aa-bb-cc-dd-ee-ff");

    // Assert
    assertTrue(configuration.contains("username=john"));
    assertTrue(configuration.contains("password=doe"));
    assertTrue(configuration.contains("domain=sip.voxloud.com"));
    assertTrue(configuration.contains("port=5060"));
    assertTrue(configuration.contains("codecs=G711,G729,OPUS"));
  }

  @Test
  void testGenerateConfigurationWithOverrideFragment() throws DeviceNotFoundException {
    // Arrange
    Device device = new Device();
    device.setMacAddress("a1-b2-c3-d4-e5-f6");
    device.setModel(Device.DeviceModel.DESK);
    device.setUsername("walter");
    device.setPassword("white");
    device.setOverrideFragment("domain=sip.anotherdomain.com\nport=5161\ntimeout=10");

    when(deviceRepository.findByMacAddress("a1-b2-c3-d4-e5-f6")).thenReturn(Optional.of(device));

    // Act
    String configuration = provisioningService.getProvisioningFile("a1-b2-c3-d4-e5-f6");

    // Assert
    assertTrue(configuration.contains("domain=sip.anotherdomain.com"));
    assertTrue(configuration.contains("port=5161"));
    assertTrue(configuration.contains("timeout=10"));
  }

  @Test
  void testDeviceNotFound() {
    // Arrange
    when(deviceRepository.findByMacAddress("unknown")).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(DeviceNotFoundException.class, () -> {
      provisioningService.getProvisioningFile("unknown");
    });
  }

}
