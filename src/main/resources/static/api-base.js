(function () {
    var meta = document.querySelector('meta[name="elearning-api-base"]');
    if (meta && meta.content && meta.content.trim()) {
        window.ELEARNING_API_BASE = meta.content.trim().replace(/\/$/, "");
        return;
    }
    if (window.location.protocol === "file:") {
        window.ELEARNING_API_BASE = "http://localhost:8080";
        return;
    }
    var localHosts = { localhost: 1, "127.0.0.1": 1, "[::1]": 1, "::1": 1 };
    if (localHosts[window.location.hostname] && window.location.port && window.location.port !== "8080") {
        window.ELEARNING_API_BASE = "http://" + window.location.hostname + ":8080";
        return;
    }
    window.ELEARNING_API_BASE = "";
})();
