var exec = require('cordova/exec');

exports.start = function (arg0, success, error) {
    exec(success, error, 'SMS', 'start', [arg0]);
};

exports.startWatching = function (arg0, success, error) {
    exec(success, error, 'SMS', 'startWatching', [arg0]);
};

exports.stop = function (arg0, success, error) {
    exec(success, error, 'SMS', 'stop', [arg0]);
};
