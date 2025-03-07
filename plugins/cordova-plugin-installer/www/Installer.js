var exec = require('cordova/exec');
var PLUGIN_NAME = 'Installer';

var installer = {

	install: function(packageid, success, error)
	{
		exec(success, error, 'Installer', 'install', [packageid]); 
	},
	checkInstallationStatus: function(success, error)
	{
		exec(success, error, 'Installer', 'checkInstallationStatus', []); 
	},
	getInstalledApps: function(success, error)
	{
		exec(success, error, 'Installer', 'getInstalledApps', []); 
	}
	
};

module.exports = installer;
