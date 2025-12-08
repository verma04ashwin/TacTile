package com.example.talktile_05

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.talktile_05.ui.HomeScreen
import com.example.talktile_05.ui.ReaderScreen
import com.example.talktile_05.ui.map.MapInteractionScreen
import com.example.talktile_05.viewmodel.MapInteractionViewModel
import com.example.talktile_05.viewmodel.ReaderViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun TalktileApp() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "home") {

        // HOME SCREEN
        composable("home") {
            HomeScreen(
                onOpenReader = { book, chapter, page ->
                    val b = URLEncoder.encode(book, StandardCharsets.UTF_8)
                    val c = URLEncoder.encode(chapter, StandardCharsets.UTF_8)
                    navController.navigate("reader/$b/$c/$page")
                }
            )
        }

        // READER SCREEN
        composable(
            "reader/{book}/{chapter}/{page}",
            arguments = listOf(
                navArgument("book") { type = NavType.StringType },
                navArgument("chapter") { type = NavType.StringType },
                navArgument("page") { type = NavType.IntType }
            )
        ) { entry ->

            val vm: ReaderViewModel = viewModel(viewModelStoreOwner = entry)

            val book = URLDecoder.decode(entry.arguments!!.getString("book")!!, StandardCharsets.UTF_8)
            val chapter = URLDecoder.decode(entry.arguments!!.getString("chapter")!!, StandardCharsets.UTF_8)
            val page = entry.arguments!!.getInt("page")

            ReaderScreen(
                book = book,
                chapter = chapter,
                page = page,
                vm = vm,
                onBack = { navController.popBackStack() },
                onOpenMap = { b, c, mapFile ->
                    val eb = URLEncoder.encode(b, StandardCharsets.UTF_8)
                    val ec = URLEncoder.encode(c, StandardCharsets.UTF_8)
                    val ef = URLEncoder.encode(mapFile, StandardCharsets.UTF_8)
                    navController.navigate("map/$eb/$ec/$ef")
                }
            )
        }

        // MAP INTERACTION SCREEN
        composable(
            route = "map/{book}/{chapter}/{mapFile}",
            arguments = listOf(
                navArgument("book") { type = NavType.StringType },
                navArgument("chapter") { type = NavType.StringType },
                navArgument("mapFile") { type = NavType.StringType }
            )
        ) { entry ->

            val book = URLDecoder.decode(entry.arguments!!.getString("book")!!, StandardCharsets.UTF_8)
            val chapter = URLDecoder.decode(entry.arguments!!.getString("chapter")!!, StandardCharsets.UTF_8)
            val mapFile = URLDecoder.decode(entry.arguments!!.getString("mapFile")!!, StandardCharsets.UTF_8)

            val vm: MapInteractionViewModel = viewModel()

            MapInteractionScreen(
                book = book,
                chapter = chapter,
                mapJsonFile = mapFile,
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
