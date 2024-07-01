var resultList = document.querySelector('ul.results');
var urlInput = document.querySelector('input[name=url]');
var errorMessage = document.getElementById('error-message');
var loadingIndicator = document.getElementById('loading');

function apiCallBack(xhr, callback) {
    if (xhr.readyState == XMLHttpRequest.DONE) {
        hideLoading();
        if (xhr.status != 200) {
            let message = xhr.status + ":" + xhr.statusText + ":" + xhr.responseText;
            alert(message);
            throw 'API call returned bad code: ' + xhr.status;
        }
        let response = xhr.responseText ? JSON.parse(xhr.responseText) : null;
        if (callback) {
            callback(response);
        }
    }
}

function updateList(response) {
    resultList.innerHTML = '';
    for (var i = 0; i < response.length; i++) {
        var img = document.createElement("img");
        img.width = 200;
        img.src = response[i];
        resultList.appendChild(img);
    }
}

function makeApiCall(url, method, obj, callback) {
    let xhr = new XMLHttpRequest();
    xhr.open(method, url);
    xhr.onreadystatechange = apiCallBack.bind(null, xhr, callback);
    xhr.send(obj ? obj instanceof FormData || obj.constructor == String ? obj : JSON.stringify(obj) : null);
}

function isValidUrl(url) {
    const urlPattern = /^https?:\/\/(?:www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b(?:[-a-zA-Z0-9()@:%_\+.~#?&\/=]*)$/;
    return urlPattern.test(url);
}

function showLoading() {
    loadingIndicator.style.display = 'flex';
}

function hideLoading() {
    loadingIndicator.style.display = 'none';
}

document.querySelector('button').addEventListener("click", function(event) {
    event.preventDefault();
    const url = urlInput.value.trim();

    if (!url || !isValidUrl(url)) {
        errorMessage.style.display = 'block';
        resultList.innerHTML = ''; // Clear previous results
    } else {
        errorMessage.style.display = 'none';
        showLoading(); // Show loading indicator before making the API call
        makeApiCall('/main?url=' + encodeURIComponent(url), 'POST', null, updateList);
    }
});