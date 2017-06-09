# bluetoothsmart 2.0a
bluetoothsmart is a library for Android that automates a majority of the redundant aspects of connected products by using annotations and encapsulation principals to allow you to model out the actual physical device as a Java (and eventually, Kotlin) class. 

# Using bluetoothsmart v2
First, you will need to model your device. This is done by having your device extend SmartDevice, and applying the proper annotation. Here is an example using Nordic Semiconductor's Blinky demo as a target device:

```java
@DeviceParameters(
    characteristics = {
        @CharacteristicDef(label = "LED", id = LED_CHAR, uuid = @UUIDRef("1525"), service = @UUIDRef(NORDIC_LED_SERVICE))
    }
)
public class NordicBlinky extends SmartDevice {
    static final String NORDIC_LED_SERVICE = "00001523-1212-efde-1523-785feabcd123";
    static final int LED_CHAR = 0x00;

    private final Intentions mEnableLED = new Intentions.Builder()
            .changeCharacteristic(LED_CHAR, (byte)0x01)
            .build();
    private final Intentions mDisableLED = new Intentions.Builder()
            .changeCharacteristic(LED_CHAR, (byte)0x00)
            .build();

    public void setLed(boolean enable) {
        startIntentions(enable?mEnableLED:mDisableLED);
    }

    public static Identifier getIdentifier() {
        return new Identifier.Builder(NordicBlinky.class)
                .uuid("00001523-1212-efde-1523-785feadcb123")
                .build();
    }
}
```

The next step is to initiate the scanner and add an identifier for your device(s), ideally within the Application class for your application. :
```java 
SmartDeviceManager.init(); 
SmartDeviceManager.setActiveContext(this);
SmartDeviceManager.getInstance().addIdentifier(NordicBlinky.getIdentifier());
```
In any event that the application context is invalidated, you will need to call setActiveContext() to re-establish the context for bluetooth communication to work.

Once you've verified the user has the necessary location permissions for Bluetooth Low Energy, simply call `SmartDeviceManager.getInstance().startScan()` to begin scanning. A callback can be configured using `SmartDeviceManager.getInstance().addScanListener()` to onChange for discovered devices, iBeacon advertisements from discovered devices, and updated devices.

