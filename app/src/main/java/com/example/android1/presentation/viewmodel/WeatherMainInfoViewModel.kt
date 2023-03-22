package com.example.android1.presentation.viewmodel

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.android1.di.DIContainer
import com.example.android1.domain.geolocation.GeoLocation
import com.example.android1.domain.geolocation.GetGeoLocationUseCase
import com.example.android1.domain.weather.GetCityIdUseCase
import com.example.android1.domain.weather.GetWeatherMainInfoUseCase
import com.example.android1.domain.weather.WeatherMainInfo
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber
import java.net.UnknownHostException

class WeatherMainInfoViewModel(
    private val getWeatherMainInfoUseCase: GetWeatherMainInfoUseCase,
    private val getCityIdUseCase: GetCityIdUseCase,
    private val getGeoLocationUseCase: GetGeoLocationUseCase
): ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean>
        get() = _loading

    private val _error = MutableLiveData<Throwable?>(null)
    val error: LiveData<Throwable?>
        get() = _error

    private val _weatherDetailedInfo = MutableLiveData<List<WeatherMainInfo>>(null)
    val weatherDetailedInfo: LiveData<List<WeatherMainInfo>>
        get() = _weatherDetailedInfo

    private val _cityId = MutableLiveData<Int?>(null)
    val cityId: LiveData<Int?>
        get() = _cityId

    val showLocationAlertDialog = MutableLiveData(false)

    val showHttpError = MutableLiveData(false)

    val showInternetConnectionError = MutableLiveData(false)

    private val _geoLocation = MutableLiveData<GeoLocation>(null)
    val geoLocation: LiveData<GeoLocation>
        get() = _geoLocation

    fun getUserLocation(arePermissionsGranted: Boolean) {
        getLocation(arePermissionsGranted)
    }

    private fun getLocation(arePermissionsGranted: Boolean) {
        viewModelScope.launch {
            try {
                getGeoLocationUseCase.invoke(arePermissionsGranted).also {
                    _geoLocation.value = it
                }
            } catch (error: Throwable) {
                _error.value = error
                Timber.e("$error")
            }
        }
    }

    fun getCityIdByName(cityName: String) {
        getCityId(cityName)
    }

    private fun getCityId(cityName: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                getCityIdUseCase.invoke(cityName).also {
                    _cityId.value = it
                    _cityId.value = null
                }
            } catch (noInternetConnection: UnknownHostException) {
                showInternetConnectionError.value = true
                _error.value = noInternetConnection
            } catch (httpException: HttpException) {
                showHttpError.value = false
                _error.value = httpException
            } catch (error: Throwable) {
                _error.value = error
            } finally {
                _loading.value = false
            }
        }
    }

    fun getNearbyCities(
        longitude: Double,
        latitude: Double,
        numberOfCities: Int,
        isLocal: Boolean
    ) {
        loadWeather(
            longitude,
            latitude,
            numberOfCities,
            isLocal
        )
    }

    private fun loadWeather(
        longitude: Double,
        latitude: Double,
        numberOfCities: Int,
        isLocal: Boolean
    ) {
        viewModelScope.launch {
            try {
                _loading.value = true
                getWeatherMainInfoUseCase.invoke(
                    mapOf(
                        "lat" to "$latitude",
                        "lon" to "$longitude",
                        "cnt" to "$numberOfCities"
                    ),
                    isLocal
                ).also {
                    _weatherDetailedInfo.value = it
                }
            } catch (noInternetConnection: UnknownHostException) {
                _error.value = noInternetConnection
            } catch (error: Throwable) {
                _error.value = error
            }
            finally {
                _loading.value = false
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val weatherInfoUseCase = DIContainer.weatherMainInfoUseCase
                val cityIdUseCase = DIContainer.cityIdUseCase
                val geoLocationUseCase = DIContainer.geoLocationUseCase
                WeatherMainInfoViewModel(weatherInfoUseCase, cityIdUseCase, geoLocationUseCase)
            }
        }
    }
}