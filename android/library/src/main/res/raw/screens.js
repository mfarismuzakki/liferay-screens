var screens = {
	screensScripts_: [],
	addScreensScript: function(screensScript) {
		this.screensScripts_.push(screensScript);
	},

	reloadScripts: function() {
		this.screensScripts_.forEach(function(scripts) {
		    scripts();
		});
	},

	isAndroid: function() {
		if (navigator.userAgent.indexOf('Android') === -1) {
			return false;
		}
		return true;
	},

	postMessage: function(namespace, message) {
		if (this.isAndroid()) {
			android.postMessage(namespace, message);
		}
		else {
			window.webkit.messageHandlers.screensDefault.postMessage([namespace, message]);
		}
	},

	listPortlets: function() {
		var portlets = window.Liferay.Portlet.list;

		var parsedPortlets = portlets.map(function(portlet) {
			var portletSplitted = portlet.split('_');
			var length = portletSplitted.length;
			if (length >= 3 && portletSplitted[length - 2] === 'INSTANCE') {
				return portletSplitted.slice(0, length - 2).join('_');
			}
			else {
				return portlet;
			}
		})
		.filter(function(x, idx, arr) {
		    return arr.indexOf(x) === idx
		})
		.join(',');

		this.postMessage("screensInternal.listPortlets", parsedPortlets);
	}
};

window.Screens = Object.create(screens);

window.Liferay.on('endNavigate', function() {
	window.Screens.reloadScripts();
});

setInterval(function() {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', window.location.origin + '/c/portal/extend_session');
    xhr.send();
}, 3 * 60 * 1000);
