package com.programmersbox.nameinfocompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.palette.graphics.Palette
import com.programmersbox.nameinfocompose.ui.theme.NameInfoComposeTheme
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.landscapist.palette.PalettePlugin
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {

    private val db by lazy { NameInfoDatabase.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NameInfoComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NameInfoCompose(viewModel { NameInfoViewModel(db) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameInfoCompose(vm: NameInfoViewModel = viewModel()) {
    Scaffold(
        topBar = {
            OutlinedTextField(
                value = vm.name,
                onValueChange = { vm.name = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                trailingIcon = { IconButton(onClick = vm::getInfo) { Icon(Icons.Default.Check, null) } },
                keyboardActions = KeyboardActions(onDone = { vm.getInfo() }),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                maxLines = 1,
                label = { Text("Enter Name:") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FirstRow(vm)
            SecondRow(vm)
            Divider()
            Recent(vm)
        }
    }
}

@Composable
fun FirstRow(vm: NameInfoViewModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ElevatedCard(modifier = Modifier.weight(1f)) {
            Crossfade(targetState = vm.state) { state ->
                when (state) {
                    NetworkState.Loading -> {
                        CircularProgressIndicator()
                    }
                    NetworkState.NotLoading -> {
                        Column(modifier = Modifier.padding(4.dp)) {
                            Text(
                                vm.ifyInfo.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                "Age: ${vm.ifyInfo.age}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }

        ElevatedCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val gender by remember {
                    derivedStateOf { vm.ifyInfo.gender }
                }
                Circle(
                    progress = gender?.probability ?: 0f,
                    strokeColor = animateColorAsState(gender?.genderColor ?: MaterialTheme.colorScheme.primary).value,
                    backgroundColor = animateColorAsState(
                        gender?.genderColorInverse ?: MaterialTheme.colorScheme.background
                    ).value,
                    textColor = animateColorAsState(gender?.genderColor ?: MaterialTheme.colorScheme.primary).value,
                    modifier = Modifier
                        .size(90.dp)
                        .padding(4.dp)
                )
                Text(gender?.capitalGender().orEmpty())
            }
        }

    }
}

@Composable
fun SecondRow(vm: NameInfoViewModel) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(vm.ifyInfo.nationality) { country ->
            ElevatedCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    var palette by remember { mutableStateOf<Palette?>(null) }

                    Circle(
                        progress = animateFloatAsState(country.probability * 100).value,
                        strokeColor = palette?.vibrantSwatch?.rgb?.toComposeColor()
                            ?: MaterialTheme.colorScheme.primary,
                        textColor = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(90.dp)
                            .padding(4.dp)
                    )

                    GlideImage(
                        modifier = Modifier.size(24.dp),
                        imageModel = { country.flagUrl },
                        component = rememberImageComponent {
                            +PalettePlugin(
                                imageModel = country.flagUrl,
                                useCache = true,
                                paletteLoadedListener = { palette = it }
                            )
                        }
                    )

                    Text(country.countryName)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Recent(vm: NameInfoViewModel) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(vm.recent) { r ->
            ElevatedCard(onClick = { vm.onRecentPress(r) }) {
                Row(modifier = Modifier.padding(4.dp)) {

                    Column {

                        Text(
                            r.name,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            "Age: ${r.age}",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        r.nationality.take(3).forEach {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlideImage(
                                    modifier = Modifier.size(12.dp),
                                    imageModel = { it.flagUrl },
                                )
                                Text("${(it.probability * 100).roundToInt()}%")
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val gender by remember {
                            derivedStateOf { r.gender }
                        }
                        Circle(
                            progress = gender?.probability ?: 0f,
                            strokeColor = gender?.genderColor ?: MaterialTheme.colorScheme.primary,
                            backgroundColor = gender?.genderColorInverse ?: MaterialTheme.colorScheme.background,
                            modifier = Modifier
                                .size(90.dp)
                                .padding(4.dp)
                        )
                        Text(gender?.capitalGender().orEmpty())
                    }
                }
            }
        }
    }
}

fun Int.toComposeColor() = Color(this)

@OptIn(ExperimentalTextApi::class)
@Composable
fun Circle(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = strokeColor,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
) {
    val progressAnimated by animateFloatAsState(progress)
    val textMeasurer = rememberTextMeasurer()
    val textLayoutResult =
        textMeasurer.measure(text = AnnotatedString("${progressAnimated.roundToInt()}%"))
    val textSize = textLayoutResult.size
    Canvas(modifier) {
        drawCircle(
            color = backgroundColor,
            radius = size.minDimension / 2,
            style = Stroke(8f)
        )

        drawArc(
            color = strokeColor,
            useCenter = false,
            startAngle = 270f,
            sweepAngle = progressAnimated / 100 * 360,
            style = Stroke(8f, cap = StrokeCap.Round),
        )

        drawText(
            textMeasurer = textMeasurer,
            text = "${progressAnimated.roundToInt()}%",
            style = TextStyle(color = textColor),
            topLeft = Offset(
                (size.width - textSize.width) / 2f,
                (size.height - textSize.height) / 2f
            ),
        )
    }
}

class NameInfoViewModel(private val db: NameInfoDatabase) : ViewModel() {

    private val service = ApiService()
    var state by mutableStateOf(NetworkState.NotLoading)
    val recent = mutableStateListOf<IfyInfo>()
    var name by mutableStateOf("")
    var ifyInfo by mutableStateOf(
        IfyInfo(
            name = "Name",
            gender = Gender(gender = "male", probability = 50f),
            age = 50,
        )
    )

    init {
        db.nameInfoDao()
            .getAll()
            .onEach {
                recent.clear()
                recent.addAll(it)
            }
            .launchIn(viewModelScope)

        runBlocking { db.nameInfoDao().getAll().firstOrNull()?.lastOrNull()?.let { ifyInfo = it } }
    }

    fun getInfo() {
        viewModelScope.launch {
            state = NetworkState.Loading
            val n = name.trim()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            recent.find { it.name == n }?.let { ifyInfo = it } ?: run {
                service.getInfo(n).fold(
                    onSuccess = {
                        ifyInfo = it
                        db.nameInfoDao().insert(it)
                    },
                    onFailure = { it.printStackTrace() }
                )
            }
            state = NetworkState.NotLoading
        }
    }

    fun onRecentPress(info: IfyInfo) {
        ifyInfo = info
    }

}

enum class NetworkState { Loading, NotLoading }

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    NameInfoComposeTheme {
        NameInfoCompose()
    }
}

@Preview(showBackground = true)
@Composable
fun CirclePreview() {
    NameInfoComposeTheme {
        Circle(
            progress = 80f,
            modifier = Modifier.size(50.dp)
        )
    }
}