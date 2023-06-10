package paintbots.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.D2
import org.jetbrains.kotlinx.multik.ndarray.data.MultiArray
import org.jetbrains.kotlinx.multik.ndarray.data.MutableMultiArray
import org.jetbrains.kotlinx.multik.ndarray.data.set
import org.jetbrains.kotlinx.multik.ndarray.operations.map
import java.awt.Color


data class State(
    val bots: Map<String, Bot>,
    val canvas: MultiArray<Int, D2>,
)

private val initialState = State(mapOf(), mk.zeros<Int>(settings.width, settings.height).map { Color.pink.rgb })
private val stateFlow = MutableStateFlow(initialState)
val currentState = stateFlow.asStateFlow()

fun <T> swapState(stateFunc: (State) -> Pair<State, T>): T {
    var result: T
    do {
        val oldState = currentState.value
        val (newState, newResult) = stateFunc(oldState)
        result = newResult
    } while (!stateFlow.compareAndSet(oldState, newState))
    return result
}

fun resetCanvas(state: State): State {
    val canvas = state.canvas.copy().map { Color.pink.rgb }
    return state.copy(canvas = canvas)
}

fun kickAll(state: State): State {
    return state.copy(bots = mapOf())
}

fun noResSwap(swapFun: (State) -> State) = { state: State ->
    Pair(swapFun(state), null)
}

fun swapPixel(state: State, x: Int, y: Int, color: Int): State {
    val newCanvas = state.canvas.copy() as MutableMultiArray
    newCanvas[x, y] = color
    return state.copy(canvas = newCanvas)
}

