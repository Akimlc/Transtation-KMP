package com.funny.translation.translate.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import com.funny.translation.AppConfig
import com.funny.translation.Consts
import com.funny.translation.GlobalTranslationConfig
import com.funny.translation.R
import com.funny.translation.bean.TranslationConfig
import com.funny.translation.helper.ClipBoardUtil
import com.funny.translation.helper.DataSaverUtils
import com.funny.translation.helper.VibratorUtils
import com.funny.translation.helper.toastOnUi
import com.funny.translation.strings.ResStrings
import com.funny.translation.translate.FunnyApplication
import com.funny.translation.translate.Language
import com.funny.translation.translate.TransActivityIntent
import com.funny.translation.translate.activity.StartCaptureScreenActivity
import com.funny.translation.translate.enabledLanguages
import com.funny.translation.translate.findLanguageById
import com.funny.translation.translate.service.CaptureScreenService
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.interfaces.OnPermissionResult
import com.lzf.easyfloat.interfaces.OnTouchRangeListener
import com.lzf.easyfloat.permission.PermissionUtils
import com.lzf.easyfloat.utils.DragUtils
import com.lzf.easyfloat.widget.BaseSwitchView
import com.tomlonghurst.roundimageview.RoundImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min


object EasyFloatUtils {
    internal const val TAG_FLOAT_BALL = "ball"
    private const val TAG_TRANS_WINDOW = "window"

    private const val TAG = "EasyFloat"
    private var vibrating = false
    private var initTransWindow = false
    var initFloatBall = false
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var translateConfigFlow =
        MutableStateFlow(TranslationConfig("", readLanguage(Consts.KEY_SOURCE_LANGUAGE, Language.AUTO), readLanguage(Consts.KEY_TARGET_LANGUAGE, Language.CHINESE)))

    private var translateJob: Job? = null

    private var inputTextFlow = MutableStateFlow("")

    private val translateEngineFlow = EngineManager.floatWindowTranslateEngineStateFlow

    fun initScreenSize() {
        AppConfig.SCREEN_WIDTH = ScreenUtils.getScreenWidth()
        AppConfig.SCREEN_HEIGHT = ScreenUtils.getScreenHeight()
    }

    private fun readLanguage(key: String, default: Language): Language {
        val language = DataSaverUtils.readData(key, default.name)
        return Language.valueOf(language)
    }

    private fun initTransWindow(view: View){
        view.layoutParams.width = (min(AppConfig.SCREEN_WIDTH, AppConfig.SCREEN_HEIGHT) * 0.9).toInt()

        val edittext = view.findViewById<EditText>(R.id.float_window_input)

        coroutineScope.launch {
            inputTextFlow.collect {
                withContext(Dispatchers.Main){
                    edittext.setText(it)
                    edittext.setSelection(it.length)
                }
            }
        }

        coroutineScope.launch {
            translateEngineFlow.collect {
                withContext(Dispatchers.Main){
                    edittext.setHint(ResStrings.translate_engine_hint.format(it.name))
                }
            }
        }

        val spinnerSource: Spinner =
            view.findViewById<Spinner?>(R.id.float_window_spinner_source).apply {
                adapter = ArrayAdapter<String>(FunnyApplication.ctx, R.layout.view_spinner_text_item).apply {
                    addAll(enabledLanguages.value.map { it.displayText })
                    setDropDownViewResource(R.layout.view_spinner_dropdown_item)
                }
                setSelection(enabledLanguages.value.indexOf(translateConfigFlow.value.sourceLanguage ?: Language.AUTO))
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        translateConfigFlow.value = translateConfigFlow.value.copy(
                            sourceString = edittext.text.trim().toString(),
                            sourceLanguage = enabledLanguages.value[position]
                        )
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }
            }

        val spinnerTarget: Spinner =
            view.findViewById<Spinner?>(R.id.float_window_spinner_target).apply {
                adapter = ArrayAdapter<String>(FunnyApplication.ctx, R.layout.view_spinner_text_item).apply {
                    addAll(enabledLanguages.value.map { it.displayText })
                    setDropDownViewResource(R.layout.view_spinner_dropdown_item)
                }
                setSelection(enabledLanguages.value.indexOf(translateConfigFlow.value.targetLanguage ?: Language.CHINESE))
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        translateConfigFlow.value = translateConfigFlow.value.copy(
                            sourceString = edittext.text.trim().toString(),
                            targetLanguage = enabledLanguages.value[position]
                        )
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }
            }

        coroutineScope.launch {
            enabledLanguages.collect {
                withContext(Dispatchers.Main){
                    spinnerSource.adapter = ArrayAdapter<String>(FunnyApplication.ctx, R.layout.view_spinner_text_item).apply {
                        addAll(enabledLanguages.value.map { it.displayText })
                        setDropDownViewResource(R.layout.view_spinner_dropdown_item)
                    }
                    spinnerSource.setSelection(enabledLanguages.value.indexOf(translateConfigFlow.value.sourceLanguage ?: Language.AUTO))

                    spinnerTarget.adapter = ArrayAdapter<String>(FunnyApplication.ctx, R.layout.view_spinner_text_item).apply {
                        addAll(enabledLanguages.value.map { it.displayText })
                        setDropDownViewResource(R.layout.view_spinner_dropdown_item)
                    }
                    spinnerTarget.setSelection(enabledLanguages.value.indexOf(translateConfigFlow.value.targetLanguage ?: Language.CHINESE))
                }
            }
        }

        val rotateAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 300
        }

        view.findViewById<ImageButton?>(R.id.float_window_exchange_button).apply {
            setOnClickListener {
                val temp = spinnerSource.selectedItemPosition
                spinnerSource.setSelection(spinnerTarget.selectedItemPosition, true)
                spinnerTarget.setSelection(temp, true)
                startAnimation(rotateAnimation)
            }
        }

        val resultText: TextView = view.findViewById(R.id.float_window_text)
        val speakBtn = view.findViewById<ImageButton>(R.id.float_window_speak_btn).apply {
            setOnClickListener {
                val txt = resultText.text
                if (txt.isNotEmpty()){
                    val language = findLanguageById(spinnerTarget.selectedItemPosition)
                    AudioPlayer.playOrPause(txt.toString(), TTSConfManager.findByLanguage(language)){
                        context.toastOnUi(ResStrings.err_speaking)
                    }
                }
            }
        }
        val speakSourceBtn = view.findViewById<TextView>(R.id.float_window_speak_source_btn).apply {
            setOnClickListener {
                val txt = edittext.text
                if (txt.isNotEmpty()){
                    val language = findLanguageById(spinnerSource.selectedItemPosition)
                    AudioPlayer.playOrPause(txt.toString(), TTSConfManager.findByLanguage(language)){
                        context.toastOnUi(ResStrings.err_speaking)
                    }
                }
            }
        }
        val copyBtn = view.findViewById<ImageButton>(R.id.float_window_copy_btn).apply {
            setOnClickListener {
                val txt = resultText.text
                if (txt.isNotEmpty()){
                    ClipBoardUtil.copy(txt)
                    context.toastOnUi(ResStrings.copied_to_clipboard)
                }
            }
        }

        view.findViewById<ImageButton?>(R.id.float_window_close).apply {
            setOnClickListener {
                EasyFloat.hide(TAG_TRANS_WINDOW)
            }
        }

        translateJob = coroutineScope.launch(Dispatchers.IO) {
            translateConfigFlow.collect {
                kotlin.runCatching {
                    if (it.sourceString!=null && it.sourceString!="") {
                        val sourceLanguage = enabledLanguages.value[spinnerSource.selectedItemPosition]
                        val targetLanguage = enabledLanguages.value[spinnerTarget.selectedItemPosition]
                        val task = TranslateUtils.createTask(
                            translateEngineFlow.value,
                            it.sourceString!!,
                            sourceLanguage,
                            targetLanguage
                        )

                        // 设置全局的翻译参数
                        with(GlobalTranslationConfig){
                            this.sourceLanguage = task.sourceLanguage
                            this.targetLanguage = task.targetLanguage
                            this.sourceString   = task.sourceString
                        }

                        withContext(Dispatchers.Main) {
                            resultText.text = ResStrings.translating
                        }
                        task.translate()
                        withContext(Dispatchers.Main) {
                            resultText.text = task.result.basic
                            if (speakBtn.visibility != View.VISIBLE){
                                speakBtn.visibility = View.VISIBLE
                            }
                            if (copyBtn.visibility != View.VISIBLE){
                                copyBtn.visibility = View.VISIBLE
                            }
                        }
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        it.printStackTrace()
                        resultText.text = ResStrings.trans_error.format(it.toString())
                    }
                }
            }
        }

        view.findViewById<TextView?>(R.id.float_window_translate).apply {
            setOnClickListener {
                val inputText = edittext.text.trim()
                if (inputText.isNotEmpty()) {
                    translateConfigFlow.value =
                        translateConfigFlow.value.copy(sourceString = inputText.toString())
                }
            }
        }

        view.findViewById<ImageButton>(R.id.float_window_open_app_btn).apply {
            setOnClickListener {
                val config = translateConfigFlow.value
                TransActivityIntent.TranslateText(text = edittext.text.trim().toString(), sourceLanguage = config.sourceLanguage!!, targetLanguage = config.targetLanguage!!, byFloatWindow = false).asIntent().let {
                    context.startActivity(it)
                }
            }
        }
    }

     fun showTransWindow(){
        if(!initTransWindow){
            EasyFloat.with(FunnyApplication.ctx)
                .setTag(TAG_TRANS_WINDOW)
                .setLayout(R.layout.layout_float_window){ view ->
                    initTransWindow(view)
                }
                .hasEditText(true)
                .setShowPattern(ShowPattern.ALL_TIME)
                .setSidePattern(SidePattern.DEFAULT)
                .setImmersionStatusBar(true)
                .setGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, 100)
                .show()
            initTransWindow = true
        }else{
            EasyFloat.show(TAG_TRANS_WINDOW)
        }
    }

    fun resetFloatBallPlace(){
        initScreenSize()
        EasyFloat.updateFloat(TAG_FLOAT_BALL, AppConfig.SCREEN_WIDTH - 200, AppConfig.SCREEN_HEIGHT * 2 / 3)
    }

    fun startTranslate(sourceString: String, sourceLanguage: Language, targetLanguage: Language){
        inputTextFlow.value = sourceString
        translateConfigFlow.value = translateConfigFlow.value.copy(sourceString, sourceLanguage, targetLanguage)
    }

    @SuppressLint("MissingPermission")
    fun setVibrator(inRange: Boolean) {
        vibrating = inRange
        if (inRange) VibratorUtils.vibrate(100)
        else VibratorUtils.cancel()
    }

    private fun _showFloatBall(){
        if(initFloatBall){
            EasyFloat.show(TAG_FLOAT_BALL)
        }else {
            var plusView: View? = null
            EasyFloat.with(FunnyApplication.ctx)
                .setTag(TAG_FLOAT_BALL)
                .setLayout(R.layout.layout_float_ball) { view ->
                    view.findViewById<RoundImageView>(R.id.float_ball_image).apply {
                        setOnClickListener {
                            showTransWindow()
                        }
                        setOnLongClickListener {
                            VibratorUtils.vibrate()
                            if (!CaptureScreenService.hasMediaProjection) {
                                // 如果没有权限，则跳转到申请权限的界面
                                StartCaptureScreenActivity.start(null)
                            } else {
                                StartCaptureScreenActivity.start(CaptureScreenService.WHOLE_SCREEN_RECT)
                            }
                            true
                        }
                    }
                    plusView = view.findViewById<ImageView>(R.id.float_ball_plus).apply {
                        alpha = 0.5f
                    }
                }
                .setShowPattern(ShowPattern.ALL_TIME)
                .setSidePattern(SidePattern.RESULT_HORIZONTAL)
                .setImmersionStatusBar(true)
                .setGravity(Gravity.END or Gravity.BOTTOM, -20, -200)
                .registerCallback {
                    drag { view, motionEvent ->
                        FloatScreenCaptureUtils.registerDrag(
                            plusView = plusView, motionEvent = motionEvent
                        )
                        // 截屏的时候就不判定删除了
                        if (FloatScreenCaptureUtils.whetherInScreenCaptureMode) return@drag
                        DragUtils.registerDragClose(motionEvent, object : OnTouchRangeListener {
                            override fun touchInRange(inRange: Boolean, view: BaseSwitchView) {
                                setVibrator(inRange)
                                view.findViewById<TextView>(com.lzf.easyfloat.R.id.tv_delete).text =
                                    if (inRange) ResStrings.release_to_delete else ResStrings.remove_float_window

                                view.findViewById<ImageView>(com.lzf.easyfloat.R.id.iv_delete)
                                    .setImageResource(
                                        if (inRange) com.lzf.easyfloat.R.drawable.icon_delete_selected
                                        else com.lzf.easyfloat.R.drawable.icon_delete_normal
                                    )
                            }

                            override fun touchUpInRange() {
                                EasyFloat.dismiss(TAG_FLOAT_BALL)
                                AppConfig.sShowFloatWindow.value = false
                                initFloatBall = false
                                CaptureScreenService.stop()
                            }
                        }, showPattern = ShowPattern.ALL_TIME)
                    }
                    dragEnd {
                        FloatScreenCaptureUtils.registerDragEnd(plusView)
                    }
                }
                .show()
            initFloatBall = true
        }
    }


    fun showFloatBall(activity : Activity){
        if(!PermissionUtils.checkPermission(FunnyApplication.ctx)) {
            AlertDialog.Builder(activity)
                .setMessage(ResStrings.tip_grant_float_window_permission)
                .setPositiveButton(ResStrings.go_to_grant) { _, _ ->
                    PermissionUtils.requestPermission(activity, object : OnPermissionResult {
                        override fun permissionResult(isOpen: Boolean) {
                            showFloatBall(activity)
                        }
                    })
                }
                .setNegativeButton(ResStrings.cancel) { _, _ -> }
                .show()
        }else{
            _showFloatBall()
        }
    }

    fun hideAllFloatWindow(){
        EasyFloat.hide(TAG_TRANS_WINDOW)
        EasyFloat.hide(TAG_FLOAT_BALL)
    }

    fun dismissAll(){
        EasyFloat.dismiss(TAG_TRANS_WINDOW)
        EasyFloat.dismiss(TAG_FLOAT_BALL)
        FloatScreenCaptureUtils.dismiss()
        translateJob?.cancel()
        CaptureScreenService.stop()
    }

    fun isShowingFloatBall() = EasyFloat.isShow(TAG_FLOAT_BALL)
}