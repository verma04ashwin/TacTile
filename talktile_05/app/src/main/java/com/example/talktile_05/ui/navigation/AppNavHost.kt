package com.example.talktile_05.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.talktile_05.ui.HomeScreen
import com.example.talktile_05.ui.ReaderScreen
import com.example.talktile_05.ui.map.CameraMapScreen
import com.example.talktile_05.viewmodel.HomeViewModel
import com.example.talktile_05.viewmodel.ReaderViewModel
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        // ---------------- HOME SCREEN -----------------------
        composable("home") {
            val vm: HomeViewModel = viewModel()
            HomeScreen(
                onOpenReader = { book, chapter, page, paragraph, line ->

                    val b = URLEncoder.encode(book, "UTF-8")
                    val c = URLEncoder.encode(chapter, "UTF-8")

                    navController.navigate("reader/$b/$c/$page/$paragraph/$line")
                },
                vm = vm
            )
        }

        // ---------------- READER SCREEN -----------------------
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

            val book = URLDecoder.decode(entry.arguments!!.getString("book")!!, "UTF-8")
            val chapter = URLDecoder.decode(entry.arguments!!.getString("chapter")!!, "UTF-8")
            val page = entry.arguments!!.getInt("page")
            val paragraph = entry.arguments!!.getInt("paragraph")
            val line = entry.arguments!!.getInt("line")

            val readerVm: ReaderViewModel = viewModel(viewModelStoreOwner = entry)

            ReaderScreen(
                book = book,
                chapter = chapter,
                page = page,
                paragraph = paragraph,
                line = line,
                vm = readerVm,
                onBack = { navController.popBackStack() },
                onOpenMap = { bName, cName, mapFile ->
                    val b = URLEncoder.encode(bName, "UTF-8")
                    val c = URLEncoder.encode(cName, "UTF-8")
                    val mf = URLEncoder.encode(mapFile, "UTF-8")
                    navController.navigate("map/$b/$c/$mf")
                }
            )
        }

        // ---------------- MAP SCREEN -----------------------
        composable(
            route = "map/{book}/{chapter}/{mapfile}",
            arguments = listOf(
                navArgument("book") { type = NavType.StringType },
                navArgument("chapter") { type = NavType.StringType },
                navArgument("mapfile") { type = NavType.StringType }
            )
        ) { entry ->

            val book = URLDecoder.decode(entry.arguments!!.getString("book")!!, "UTF-8")
            val chapter = URLDecoder.decode(entry.arguments!!.getString("chapter")!!, "UTF-8")
            val mapfile = URLDecoder.decode(entry.arguments!!.getString("mapfile")!!, "UTF-8")

            CameraMapScreen(
                book = book,
                chapter = chapter,
                mapJsonFile = mapfile,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
