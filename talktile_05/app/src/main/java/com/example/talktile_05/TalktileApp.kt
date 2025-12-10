package com.example.talktile_05

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.talktile_05.ui.HomeScreen
import com.example.talktile_05.ui.ReaderScreen
import com.example.talktile_05.ui.map.CameraMapScreen
import com.example.talktile_05.viewmodel.ReaderViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun TalktileApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // ---------------- HOME SCREEN ----------------
        composable("home") {
            HomeScreen(
                onOpenReader = { book, chapter, page, paragraph, line ->

                    val b = URLEncoder.encode(book, StandardCharsets.UTF_8)
                    val c = URLEncoder.encode(chapter, StandardCharsets.UTF_8)

                    navController.navigate("reader/$b/$c/$page/$paragraph/$line")
                }
            )
        }

        // ---------------- READER SCREEN ----------------
        composable(
            route = "reader/{book}/{chapter}/{page}/{paragraph}/{line}",
            arguments = listOf(
                navArgument("book") { type = NavType.StringType },
                navArgument("chapter") { type = NavType.StringType },
                navArgument("page") { type = NavType.IntType },
                navArgument("paragraph") { type = NavType.IntType },
                navArgument("line") { type = NavType.IntType }
            )
        ) { entry ->

            val vm: ReaderViewModel = viewModel(viewModelStoreOwner = entry)

            val book = URLDecoder.decode(entry.arguments!!.getString("book")!!, StandardCharsets.UTF_8)
            val chapter = URLDecoder.decode(entry.arguments!!.getString("chapter")!!, StandardCharsets.UTF_8)

            val page = entry.arguments!!.getInt("page")
            val paragraph = entry.arguments!!.getInt("paragraph")
            val line = entry.arguments!!.getInt("line")

            ReaderScreen(
                book = book,
                chapter = chapter,
                page = page,
                paragraph = paragraph,
                line = line,
                vm = vm,
                onBack = { navController.popBackStack() },
                onOpenMap = { bName, cName, mapFile ->
                    val b = URLEncoder.encode(bName, StandardCharsets.UTF_8)
                    val c = URLEncoder.encode(cName, StandardCharsets.UTF_8)
                    val f = URLEncoder.encode(mapFile, StandardCharsets.UTF_8)
                    navController.navigate("map/$b/$c/$f")
                }
            )
        }

        // ---------------- MAP SCREEN ----------------
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

            CameraMapScreen(
                book = book,
                chapter = chapter,
                mapJsonFile = mapFile,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
