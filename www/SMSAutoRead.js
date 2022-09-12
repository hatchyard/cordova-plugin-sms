var exec = require('cordova/exec');

exports.start = function (arg0, success, error) {
    exec(success, error, 'SMSAutoRead', 'start', [arg0]);
};

exports.startWatching = function (arg0, success, error) {
    exec(success, error, 'SMSAutoRead', 'startWatching', [arg0]);
};

exports.stop = function (arg0, success, error) {
    exec(success, error, 'SMSAutoRead', 'stop', [arg0]);
};
