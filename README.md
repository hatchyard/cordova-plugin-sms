# cordova-plugin-sms
This is sms plugin for cordova

# To run this plugin, you can use following method

```
cordova.plugins.SMS.start([], onSuccessCallback, onErrorCallback)
```

You can access the callbacks like following code.
```
function onSuccessCallback(response) {
    console.log("This is successful callback", response);
}

function onErrorCallback(response) {
    console.log("This is failure callback", response);
}
```
