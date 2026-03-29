document.addEventListener('DOMContentLoaded', function() {
    loadWeatherData();
});

function loadWeatherData() {
    fetch('/api/weather/component')
        .then(response => response.json())
        .then(data => {
            updateWeatherUI(data);
        })
        .catch(error => {
            console.error('加载天气数据失败:', error);
            showError();
        });
}

function updateWeatherUI(data) {
    document.getElementById('weather-date').textContent = data.currentDate;
    document.getElementById('weather-location').textContent = data.location;
    document.getElementById('weather-time').textContent = data.timePoint || '';
    document.getElementById('weather-greeting-text').textContent = data.greeting || '';
    document.getElementById('weather-icon').src = data.weatherIcon;
    document.getElementById('weather-temp').textContent = data.currentTemp;
    document.getElementById('weather-desc').textContent = data.todayWeatherDesc;
    document.getElementById('weather-today-range').textContent =
        data.todayLowTemp + ' ~ ' + data.todayHighTemp;
    document.getElementById('weather-tomorrow-range').textContent =
        data.tomorrowLowTemp + ' ~ ' + data.tomorrowHighTemp;
    document.getElementById('weather-tomorrow-desc').textContent = data.tomorrowWeatherDesc;
}

function showError() {
    document.getElementById('weather-location').textContent = '加载失败';
}
