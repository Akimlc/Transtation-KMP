package com.funny.trans.login.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funny.translation.AppConfig
import com.funny.translation.bean.UserInfoBean
import com.funny.translation.helper.SimpleAction
import com.funny.translation.helper.UserUtils
import com.funny.translation.helper.VibratorUtils
import com.funny.translation.helper.biomertic.BiometricUtils
import com.funny.translation.helper.toastOnUi
import com.funny.translation.kmp.KMPActivity
import com.funny.translation.kmp.LocalKMPContext
import com.funny.translation.kmp.NavController
import com.funny.translation.kmp.viewModel
import com.funny.translation.login.strings.ResStrings
import com.funny.translation.network.ServiceCreator
import com.funny.translation.network.api
import com.funny.translation.ui.MarkdownText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal const val WIDTH_FRACTION = 0.8f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoginPage(
    navController: NavController,
    onLoginSuccess: (UserInfoBean) -> Unit,
) {
    val vm: LoginViewModel = viewModel()

    Column(
        Modifier
            .fillMaxHeight()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        val pagerState = rememberPagerState(pageCount = { 2 })
        val scope = rememberCoroutineScope()
        fun changePage(index: Int) = scope.launch {
            pagerState.animateScrollToPage(index)
        }
        TabRow(
            pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth(WIDTH_FRACTION)
                .clip(shape = RoundedCornerShape(12.dp))
                .background(Color.Transparent),
            containerColor = Color.Transparent
            //backgroundColor = Color.Unspecified
        ) {
            Tab(pagerState.currentPage == 0, onClick = { changePage(0) }) {
                Text(
                    ResStrings.login,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Tab(pagerState.currentPage == 1, onClick = { changePage(1) }) {
                Text(
                    ResStrings.register,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        var privacyGranted by remember { mutableStateOf(false) }
        val shrinkAnim = remember { Animatable(0f) }
        val context = LocalKMPContext.current
        val remindToGrantPrivacyAction = remember {
            {
                scope.launch {
                    intArrayOf(20, 0).forEach {
                        shrinkAnim.animateTo(it.toFloat(), spring(Spring.DampingRatioHighBouncy))
                    }
                }
                VibratorUtils.vibrate(70)
                context.toastOnUi(ResStrings.tip_confirm_privacy_first)
            }
        }

        HorizontalPager(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = pagerState,
        ) { page ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (page) {
                    0 -> LoginForm(
                        navController,
                        vm,
                        onLoginSuccess = onLoginSuccess,
                        privacyGranted = privacyGranted,
                        remindToGrantPrivacyAction = remindToGrantPrivacyAction
                    )

                    1 -> RegisterForm(
                        vm,
                        onRegisterSuccess = { changePage(0) },
                        privacyGranted = privacyGranted,
                        remindToGrantPrivacyAction = remindToGrantPrivacyAction
                    )
                }
            }
        }
        Row(
            Modifier
                .padding(8.dp)
                .offset { IntOffset(0, shrinkAnim.value.roundToInt()) },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = privacyGranted, onCheckedChange = { privacyGranted = it })
            MarkdownText(
                ResStrings.tip_agree_privacy.format(privacy = ServiceCreator.getPrivacyUrl(), userAgreement = ServiceCreator.getUserAgreementUrl()),
                color = contentColorFor(backgroundColor = MaterialTheme.colorScheme.background).copy(
                    0.8f
                ),
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun LoginForm(
    navController: NavController,
    vm: LoginViewModel,
    privacyGranted: Boolean = false,
    onLoginSuccess: (UserInfoBean) -> Unit = {},
    remindToGrantPrivacyAction: () -> Unit = {},
) {
    val context = LocalKMPContext.current
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxWidth(WIDTH_FRACTION)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InputUsernameWrapper(
            vm,
            if (vm.passwordType == PASSWORD_TYPE_FINGERPRINT) ImeAction.Done else ImeAction.Next
        )
        if (vm.shouldVerifyEmailWhenLogin) {
            InputEmailWrapper(modifier = Modifier.fillMaxWidth(), vm = vm, initialSent = true)
            Spacer(modifier = Modifier.height(4.dp))
        }
        if (vm.passwordType == PASSWORD_TYPE_PASSWORD) {
            InputPasswordWrapper(vm = vm)
        } else CompletableButton(
            onClick = {
                if (supportBiometric) {
                    BiometricUtils.validateFingerPrint(
                        context as KMPActivity,
                        username = vm.username,
                        did = AppConfig.androidId,
                        onNotSupport = { msg: String -> context.toastOnUi(msg) },
                        onFail = { context.toastOnUi(ResStrings.validate_fingerprint_failed_unknown_reason) },
                        onSuccess = { encryptedInfo, iv ->
                            context.toastOnUi(ResStrings.validate_fingerprint_success)
                            vm.finishValidateFingerPrint = true
                            vm.encryptedInfo = encryptedInfo
                            vm.iv = iv
                        },
                        onError = { errorCode, errorMsg ->
                            context.toastOnUi(
                                ResStrings.validate_fingerprint_failed_with_msg.
                                    format(errorCode.toString(), errorMsg.toString())
                            )
                        },
                        onNewFingerPrint = { email ->
                            if (email.isNotEmpty()) {
                                try {
                                    scope.launch {
                                        vm.shouldVerifyEmailWhenLogin = true
                                        vm.email = email
                                        vm.secureEmail = true
                                        // BiometricUtils.uploadFingerPrint(username = vm.username)

                                        api(UserUtils.userService::sendVerifyEmail, vm.username, email)
                                    }
                                } catch (e: Exception) {
                                    context.toastOnUi(ResStrings.error_sending_email)
                                }
                            }
                        },
                        onUsePassword = {
                            vm.passwordType = PASSWORD_TYPE_PASSWORD
                            vm.password = ""
                        }
                    )
                } else {
                    context.toastOnUi(ResStrings.fingerprint_not_support)
                    vm.passwordType = PASSWORD_TYPE_PASSWORD
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = vm.isValidUsername,
            completed = vm.finishValidateFingerPrint
        ) {
            Text(ResStrings.validate_fingerprint)
        }
        ExchangePasswordType(
            passwordType = vm.passwordType,
            resetFingerprintAction = vm::resetFingerprint
        ) {
            vm.passwordType = it
        }
        Spacer(modifier = Modifier.height(12.dp))

        // 因为下面的表达式变化速度快过UI的变化速度，为了减少重组次数，此处使用 derivedStateOf
        val enabledLogin by remember {
            derivedStateOf {
                if (vm.shouldVerifyEmailWhenLogin) {
                    vm.isValidUsername && vm.finishValidateFingerPrint && vm.isValidEmail && vm.verifyCode.length == 6
                } else {
                    when (vm.passwordType) {
                        PASSWORD_TYPE_FINGERPRINT -> vm.isValidUsername && vm.finishValidateFingerPrint
                        PASSWORD_TYPE_PASSWORD -> vm.isValidUsername && UserUtils.isValidPassword(vm.password)
                        else -> false
                    }
                }
            }
        }
        Button(
            onClick = {
                if (!privacyGranted) {
                    remindToGrantPrivacyAction()
                    return@Button
                }
                vm.login(
                    onSuccess = {
                        onLoginSuccess(it)
                    },
                    onError = { msg ->
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabledLogin
        ) {
            Text(ResStrings.login)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = {
                    navController.navigate(LoginRoute.FindUsernamePage.route)
                }
            ) {
                Text(ResStrings.forgot_username)
            }
            TextButton(
                onClick = {
                    navController.navigate(LoginRoute.ResetPasswordPage.route)
                }
            ) {
                Text(ResStrings.forgot_password)
            }
        }
    }
}

@Composable
private fun RegisterForm(
    vm: LoginViewModel,
    privacyGranted: Boolean,
    onRegisterSuccess: () -> Unit = {},
    remindToGrantPrivacyAction: () -> Unit,
) {
    val context = LocalKMPContext.current
    Column(
        Modifier
            .fillMaxWidth(WIDTH_FRACTION)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InputUsernameWrapper(vm)
        InputEmailWrapper(modifier = Modifier.fillMaxWidth(), vm = vm)
        // 邀请码
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = vm.inviteCode,
            onValueChange = { vm.inviteCode = it },
            isError = vm.inviteCode != "" && !vm.isValidInviteCode,
            label = { Text(text = ResStrings.invite_code) },
            placeholder = { Text(ResStrings.please_input_invite_code) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (vm.passwordType == PASSWORD_TYPE_PASSWORD) {
            InputPasswordWrapper(vm = vm)
        } else {
            CompletableButton(
                onClick = {
                    if (supportBiometric) {
                        BiometricUtils.setFingerPrint(
                            context as KMPActivity,
                            username = vm.username,
                            did = AppConfig.androidId,
                            onNotSupport = { msg: String -> context.toastOnUi(msg) },
                            onFail = { context.toastOnUi(ResStrings.validate_fingerprint_failed_unknown_reason) },
                            onSuccess = { encryptedInfo, iv ->
                                context.toastOnUi(ResStrings.add_fingerprint_success)
                                vm.finishSetFingerPrint = true
                                vm.encryptedInfo = encryptedInfo
                                vm.iv = iv
                            },
                            onError = { errorCode, errorMsg ->
                                context.toastOnUi(
                                    ResStrings.validate_fingerprint_failed_with_msg.format(
                                        errorCode.toString(), errorMsg.toString()
                                    )
                                )
                            },
                            onUsePassword = {
                                vm.passwordType = PASSWORD_TYPE_PASSWORD
                                vm.password = ""
                            }
                        )
                    } else {
                        context.toastOnUi(ResStrings.fingerprint_not_support)
                        vm.passwordType = PASSWORD_TYPE_PASSWORD
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = vm.isValidUsername,
                completed = vm.finishSetFingerPrint
            ) {
                Text(ResStrings.add_fingerprint)
            }
        }
        ExchangePasswordType(
            passwordType = vm.passwordType
        ) { vm.passwordType = it }
        Spacer(modifier = Modifier.height(12.dp))
        val enableRegister by remember {
            derivedStateOf {
                if (vm.passwordType == PASSWORD_TYPE_FINGERPRINT)
                    vm.isValidUsername && vm.isValidEmail && vm.verifyCode.length == 6 && vm.finishSetFingerPrint
                else
                    vm.isValidUsername && vm.isValidEmail && vm.verifyCode.length == 6 && UserUtils.isValidPassword(vm.password)
            }
        }
        Button(
            onClick = {
                if (!privacyGranted) {
                    remindToGrantPrivacyAction()
                    return@Button
                }
                vm.register(
                    onSuccess = onRegisterSuccess,
                    onError = { msg ->
                        context.toastOnUi(ResStrings.register_failed_with_msg.format(msg))
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enableRegister
        ) {
            Text(ResStrings.register)
        }
    }
}

@Composable
private fun ExchangePasswordType(
    passwordType: String,
    resetFingerprintAction: SimpleAction? = null,
    updatePasswordType: (String) -> Unit,
) {
    if (passwordType == PASSWORD_TYPE_PASSWORD && supportBiometric) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            modifier = Modifier.clickable { updatePasswordType(PASSWORD_TYPE_FINGERPRINT) },
            text = ResStrings.change_to_fingerprint,
            style = MaterialTheme.typography.labelSmall
        )
    } else if (passwordType == PASSWORD_TYPE_FINGERPRINT) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.clickable { updatePasswordType(PASSWORD_TYPE_PASSWORD) },
                text = ResStrings.change_to_password,
                style = MaterialTheme.typography.labelSmall
            )
            if (resetFingerprintAction != null) {
                Text(text = " | ")
                // 重设指纹
                Text(
                    modifier = Modifier.clickable(onClick = resetFingerprintAction),
                    text = ResStrings.reset_fingerprint,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}


@Composable
private fun InputUsernameWrapper(
    vm: LoginViewModel,
    imeAction: ImeAction = ImeAction.Next,
) {
    InputUsername(
        usernameProvider = vm::username,
        updateUsername = vm::updateUsername,
        isValidUsernameProvider = vm::isValidUsername,
        imeAction = imeAction
    )
}

@Composable
private fun InputEmailWrapper(
    modifier: Modifier,
    vm: LoginViewModel,
    initialSent: Boolean = false,
) {
    val context = LocalKMPContext.current
    InputEmail(
        modifier = modifier,
        value = vm.displayEmail,
        onValueChange = { vm.email = it },
        isError = vm.email != "" && !vm.isValidEmail,
        verifyCode = vm.verifyCode,
        onVerifyCodeChange = { vm.verifyCode = it },
        initialSent = initialSent,
        onClick = { vm.sendVerifyEmail(context) }
    )
}

@Composable
fun InputEmail(
    modifier: Modifier = Modifier,
    value: String = "",
    onValueChange: (String) -> Unit = {},
    isError: Boolean = false,
    verifyCode: String,
    onVerifyCodeChange: (String) -> Unit = {},
    initialSent: Boolean,
    onClick: () -> Unit
) {
    Column(modifier) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = { onValueChange(it.lowercase()) },
            isError = isError,
            label = { Text(text = ResStrings.Email) },
            placeholder = { Text(ResStrings.please_input_validate_email) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            trailingIcon = {
                CountDownTimeButton(
                    modifier = Modifier.weight(1f),
                    onClick = onClick,
                    enabled = value != "" && !isError,
                    initialSent = initialSent // 当需要
                )
            },
            supportingText = {
                if (isError) {
                    Text(text = ResStrings.please_input_validate_email, style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.error))
                }
            }
        )
        val isVerifyCodeError by remember(verifyCode) {
            derivedStateOf { verifyCode != "" && verifyCode.length != 6 }
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = verifyCode,
            onValueChange = onVerifyCodeChange,
            isError = isVerifyCodeError,
            label = { Text(text = ResStrings.verify_code) },
            placeholder = { Text(ResStrings.please_input_verify_code) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            supportingText = {
                if (isVerifyCodeError) {
                    Text(text = ResStrings.please_input_verify_code)
                }
            }
        )
    }
}

@Composable
private fun InputPasswordWrapper(
    vm: LoginViewModel
) {
    InputPassword(
        passwordProvider = vm::password,
        updatePassword = vm::updatePassword,
    )
}

/**
 * 带倒计时的按钮
 *
 */
@Composable
fun CountDownTimeButton(
    modifier: Modifier,
    onClick: () -> Unit,
    countDownTime: Int = 60,
    text: String = ResStrings.get_verify_code,
    enabled: Boolean = true,
    initialSent: Boolean = false
) {
    var time by remember { mutableStateOf(countDownTime) }
    var isTiming by remember { mutableStateOf(initialSent) }
    LaunchedEffect(isTiming) {
        while (isTiming) {
            delay(1000)
            time--
            if (time == 0) {
                isTiming = false
                time = countDownTime
            }
        }
    }
    TextButton(
        onClick = {
            if (!isTiming) {
                isTiming = true
                onClick()
            }
        },
        modifier = modifier,
        enabled = enabled && !isTiming
    ) {
        Text(text = if (isTiming) "${time}s" else text)
    }
}