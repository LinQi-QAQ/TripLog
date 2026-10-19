package com.linqi.triplog.ui.edit

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linqi.triplog.ai.AiClient
import com.linqi.triplog.ai.TicketInfo
import com.linqi.triplog.data.image.ImageStore
import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.data.local.VehicleType
import com.linqi.triplog.data.repo.TripRepository
import com.linqi.triplog.di.AppContainer
import com.linqi.triplog.remind.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class EditForm(
    val id: Long = 0,
    val vehicleType: String = VehicleType.TRAIN,
    val fromStation: String = "",
    val toStation: String = "",
    val departTime: Long? = null,
    val arriveTime: Long? = null,
    val number: String = "",
    val seat: String = "",
    val price: String = "",
    val passenger: String = "",
    val note: String = "",
    val ticketImagePath: String = "",
    val remindBeforeMinutes: Int = 0
) {
    val valid: Boolean
        get() = fromStation.isNotBlank() && toStation.isNotBlank() && departTime != null

    fun toTrip(): Trip {
        val now = System.currentTimeMillis()
        val priceText = price.trim()
        return Trip(
            id = id,
            vehicleType = vehicleType,
            fromStation = fromStation.trim(),
            toStation = toStation.trim(),
            departTime = departTime,
            arriveTime = arriveTime,
            number = number.trim(),
            seat = seat.trim(),
            price = priceText,
            // 入库时把票价解析成数值并存下来，统计直接读，不用每次重解析字符串。
            // 解析不出来存 null，表示「没有金额」，与「0 元」区分开。
            priceValue = com.linqi.triplog.data.stats.FareParser.parseOrNull(priceText),
            passenger = passenger.trim(),
            note = note.trim(),
            ticketImagePath = ticketImagePath,
            remindBeforeMinutes = remindBeforeMinutes,
            createdAt = if (id == 0L) now else now,
            updatedAt = now
        )
    }

    companion object {
        fun from(trip: Trip): EditForm = EditForm(
            id = trip.id,
            vehicleType = trip.vehicleType,
            fromStation = trip.fromStation,
            toStation = trip.toStation,
            departTime = trip.departTime,
            arriveTime = trip.arriveTime,
            number = trip.number,
            seat = trip.seat,
            price = trip.price,
            passenger = trip.passenger,
            note = trip.note,
            ticketImagePath = trip.ticketImagePath,
            remindBeforeMinutes = trip.remindBeforeMinutes
        )
    }
}

data class EditUiState(
    val form: EditForm = EditForm(),
    val recognizing: Boolean = false,
    val recognizeError: String = "",
    val recognizeSuccess: Boolean = false,
    val saved: Boolean = false,
    val loaded: Boolean = true,
    val aiConfigured: Boolean = false
)

class TripEditViewModel(
    app: Application,
    private val repo: TripRepository,
    private val aiClient: AiClient,
    aiConfigured: Boolean
) : AndroidViewModel(app) {

    /** 反射创建用：AndroidViewModelFactory 只认 (Application) 单参构造函数。 */
    constructor(app: Application) : this(
        app,
        AppContainer.of(app).tripRepository,
        AppContainer.of(app).aiClient,
        AppContainer.of(app).settings.aiConfigured()
    )

    private val _state = MutableStateFlow(EditUiState(aiConfigured = aiConfigured))
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    fun load(tripId: Long?) {
        if (tripId == null || tripId <= 0) return
        viewModelScope.launch {
            val trip = repo.getById(tripId)
            if (trip != null) {
                _state.value = _state.value.copy(form = EditForm.from(trip))
            }
        }
    }

    fun update(transform: (EditForm) -> EditForm) {
        _state.value = _state.value.copy(form = transform(_state.value.form))
    }

    /** 识别车票截图：压缩存盘，调 AI，结果预填表单。 */
    fun recognize(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(recognizing = true, recognizeError = "", recognizeSuccess = false)
            val path = ImageStore.saveCompressed(getApplication(), uri)
            if (path == null) {
                _state.value = _state.value.copy(recognizing = false, recognizeError = "图片读取失败，请换一张")
                return@launch
            }
            when (val result = aiClient.recognizeTicket(path)) {
                is AiClient.Result.Failure -> {
                    _state.value = _state.value.copy(
                        recognizing = false,
                        recognizeError = result.message,
                        form = _state.value.form.copy(ticketImagePath = path)
                    )
                }
                is AiClient.Result.Success -> {
                    _state.value = _state.value.copy(
                        recognizing = false,
                        recognizeSuccess = true,
                        form = applyInfo(_state.value.form, result.info, path)
                    )
                }
            }
        }
    }

    private fun applyInfo(form: EditForm, info: TicketInfo, path: String): EditForm {
        var f = form.copy(ticketImagePath = path)
        info.vehicle_type?.let { v ->
            if (VehicleType.all.contains(v)) f = f.copy(vehicleType = v)
        }
        info.from_station?.let { f = f.copy(fromStation = it) }
        info.to_station?.let { f = f.copy(toStation = it) }
        info.depart_time?.let { f = f.copy(departTime = parseTime(it)) }
        info.arrive_time?.let { f = f.copy(arriveTime = parseTime(it)) }
        info.number?.let { f = f.copy(number = it) }
        info.seat?.let { f = f.copy(seat = it) }
        info.price?.let { f = f.copy(price = it) }
        info.passenger?.let { f = f.copy(passenger = it) }
        info.note?.let { f = f.copy(note = it) }
        return f
    }

    private fun parseTime(text: String): Long? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy/M/d H:mm",
            "yyyy年M月d日 HH:mm"
        )
        for (p in patterns) {
            try {
                return LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern(p))
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun save() {
        val form = _state.value.form
        if (!form.valid) return
        viewModelScope.launch {
            val trip = form.toTrip()
            if (trip.id == 0L) {
                val newId = repo.insert(trip)
                ReminderScheduler.schedule(getApplication(), trip.copy(id = newId))
            } else {
                repo.update(trip)
                ReminderScheduler.cancel(getApplication(), trip.id)
                ReminderScheduler.schedule(getApplication(), trip)
            }
            _state.value = _state.value.copy(saved = true)
        }
    }

    fun deleteCurrent() {
        val form = _state.value.form
        if (form.id <= 0) return
        viewModelScope.launch {
            repo.getById(form.id)?.let { trip ->
                repo.delete(trip)
                ImageStore.delete(trip.ticketImagePath)
                ReminderScheduler.cancel(getApplication(), trip.id)
            }
            _state.value = _state.value.copy(saved = true)
        }
    }
}
