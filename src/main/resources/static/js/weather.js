// 天气组件：从 API 获取天气数据并渲染到页面
document.addEventListener('DOMContentLoaded', function() {
    loadWeatherData();
});

function loadWeatherData() {
    fetch('/api/weather/component')
        .then(function(response) { return response.json(); })
        .then(function(data) {
            updateWeatherUI(data);
        })
        .catch(function() {
            document.getElementById('weather-location').innerHTML = '<i class="bi bi-geo-alt"></i><span>加载失败</span>';
        });
}

function updateWeatherUI(data) {
    // location
    var locEl = document.getElementById('weather-location');
    locEl.innerHTML = '<i class="bi bi-geo-alt"></i><span>' + (data.location || '--') + '</span>';

    // greeting: time + greeting text
    var greetEl = document.getElementById('weather-greeting');
    var time = data.timePoint || '';
    var greeting = data.greeting || '';
    greetEl.textContent = time ? (time + '  ' + greeting) : greeting;

    // icon (emoji)
    document.getElementById('weather-icon').textContent = data.weatherIcon || '☀️';

    // temp
    document.getElementById('weather-temp').textContent = data.currentTemp || '--°';

    // today description
    document.getElementById('weather-desc').textContent = data.todayWeatherDesc || '';

    // today range
    document.getElementById('weather-today-range').textContent =
        (data.todayLowTemp || '--') + ' ~ ' + (data.todayHighTemp || '--');

    // tomorrow range
    document.getElementById('weather-tomorrow-range').textContent =
        (data.tomorrowLowTemp || '--') + ' ~ ' + (data.tomorrowHighTemp || '--');

    // tomorrow desc
    document.getElementById('weather-tomorrow-desc').textContent = data.tomorrowWeatherDesc || '';
}
