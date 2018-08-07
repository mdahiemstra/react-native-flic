# react-native-flic

React-Native wrapper for the PBF Flic buttons.

The wrapper basically proxies every event received from the Flic SDK to React-Native.

## Getting started

`$ npm install react-native-flic --save`

### Mostly automatic installation

`$ react-native link react-native-flic`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-flic` and add `RNFlic.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNFlic.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.opencii.rn.flic.RNFlicPackage;` to the imports at the top of the file
  - Add `new RNFlicPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-flic'
  	project(':react-native-flic').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-flic/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-flic')
  	```

## Setup Flic Manager

*Important* Assuming you have the PBF Flic SDK library, you will have to add it manually in iOS (Xcode). For Android, place the library in `react-native-flic/android/libs`.

You will need to configure the Flic Manager to use your App ID and App Secret.

*Android*
- `APP_ID` in `react-native-flic/android/src/main/java/com/opencii/rn/flic/RNFlicModule.java`
- `APP_SECRET` in `react-native-flic/android/src/main/java/com/opencii/rn/flic/RNFlicModule.java`

*iOS*
- `appID` in `react-native-flic/ios/RNFlic.m`
- `appSecret` in `react-native-flic/ios/RNFlic.m`

## Flic Events in React-Native

I tried to keep the events the same for Android and iOS, but the events received from Flic SDK are not the same on both platforms.

| Event name            | Description              | Properties | Platform      |
|-----------------------|--------------------------|------------|---------------|
| `GET_KNOWN_BUTTONS`     | Sent by getKnownButtons method | - | iOS, Android |
| `SEARCH_BUTTON_START`     | Sent by searchButtons method | - | iOS, Android |
| `SEARCH_BUTTON_TIMEOUT`     | Sent by searchButtons method | - | iOS, Android |
| `BUTTON_READY` | Button discoverd and connected | `rssi`, `buttonId` | iOS, Android | 
| `MANAGER_RESTORED` | Flic Manager instance restored | - | iOS | 
| `BLUETOOTH_SWITCHED_STATE` | Bluetooth state changed | `state` | iOS | 
| `BUTTON_PRESSED` | Flic Button pressed | - | iOS, Android | 
| `BUTTON_RELEASED` | Flic Button released | - | Android | 
| `BUTTON_CONNECTED` | Flic Button connected | `buttonId` | iOS, Android | 
| `BUTTON_DISCONNECTED` | Flic Button disconnected | `buttonId`, `error` | iOS, Android | 
| `BUTTON_CONNECTING_FAILED` | Flic Button failed to connect | `buttonId` | Android | 
| `FOUND_PRIVATE_BUTTON` | Private Flic Button found | `buttonId` | Android | 
| `FOUND_PUBLIC_BUTTON` | Public Flic Button found | `buttonId` | Android | 
| `CONNECTION_ESTABLISHED` | Flic Button connected | `buttonId` | Android | 
| `BUTTON_ADDED_SUCCESS` | Flic Button added | `buttonId` | Android | 
| `BUTTON_ADDED_FAILED` | Flic Button add failed | - | Android | 

## Flic Methods in React-Native

There are some methods available in RN, you can easily add your own.  

| Method name            | Description              | Arguments | Platform      |
|-----------------------|--------------------------|------------|---------------|
| `Flic.getKnownButtons`     | Get known buttons and connect them, returns event `GET_KNOWN_BUTTONS` | | iOS, Android |
| `Flic.makeCall(number)`     | Make a phone call from RN | `number` | Android |
| `Flic.searchButtons`     | Search for new buttons, returns event `FOUND_PRIVATE_BUTTON` or `FOUND_PUBLIC_BUTTON` and connects the button | - | iOS, Android |

## Usage

This example uses Native-Base for UI components.

```javascript
import React, { Component } from 'react';
import { View, NativeEventEmitter } from 'react-native';
import { Button, Text, Badge } from 'native-base';
import Flic from 'react-native-flic';

class FlicExample extends Component {
  constructor(props) {
    super(props);
    this.eventListener = false;
  }

  state = {
    status: '',
    pressCount: 0,
    buttonConnected: false,
  }

  componentDidMount() {
    if (!this.eventListener) {
      const FlicEvents = new NativeEventEmitter(Flic);
      FlicEvents.addListener('FLIC', event => this.receivedEvent(event));
    }

    // Pair known buttons
    Flic.getKnownButtons('FLIC');
  }

  receivedEvent(response) {
    const { pressCount } = this.state;

    console.log('Response received:', response);

    if (response.event) {
      this.setState({ status: response.event });

      switch (response.event) {
        case 'BUTTON_PRESSED':
          // Do whatever you want.
          this.setState({ pressCount: pressCount + 1 });
          break;
        case 'BUTTON_CONNECTED':
          this.setState({ buttonConnected: true });
          break;

        default:
      }
    }
  }

  render() {  
    const { buttonConnected, status, pressCount } = this.state;

    return (
      <View style={{ flex: 1 }}>
        <Button
          block
          key="search-flic-buttons"
          style={{ marginBottom: 10 }}
          onPress={() => Flic.searchButtons('FLIC')}
        >
          <Text>Search for Flic buttons</Text>
        </Button>

        <Button
          block
          key="get-flic-buttons"
          style={{ marginBottom: 10 }}
          onPress={() => Flic.getKnownButtons('FLIC')}
        >
          <Text>Pair Flic buttons</Text>
        </Button>

        {buttonConnected &&
          <Badge success style={{ marginBottom: 10, alignSelf: 'center' }}>
            <Text>Flic button Connected</Text>
          </Badge>
        }

        <Text style={{ marginTop: 10, marginBottom: 10 }}>Flic event: {status}</Text>

        {pressCount > 0 &&
          <Text style={{ fontSize: 24 }}>{pressCount} time(s) pressed</Text>
        }
      </View>
    );
  }
}

export default FlicExample;
```
  