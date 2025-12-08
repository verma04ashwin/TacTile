package com.example.talktile_05.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.talktile_05.ui.HomeScreen
import com.example.talktile_05.ui.ReaderScreen
import com.example.talktile_05.ui.map.MapInteractionScreen
import com.example.talktile_05.viewmodel.HomeViewModel
import com.example.talktile_05.viewmodel.ReaderViewModel
import com.example.talktile_05.viewmodel.MapInteractionViewModel
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            val vm: HomeViewModel = viewModel()
            HomeScreen(
                onOpenReader = { book, chapter, page ->
                    val b = URLEncoder.encode(book, "UTF-8")
                    val c = URLEncoder.encode(chapter, "UTF-8")
                    navController.navigate("reader/$b/$c/$page")
                },
                vm = vm
            )
        }

        composable(
            route = "reader/{book}/{chapter}/{page}",
            arguments = listOf(
                navArgument("book") { type = NavType.StringType },
                navArgument("chapter") { type = NavType.StringType },
                navArgument("page") { type = NavType.IntType }
            )
        ) { entry ->
            val book = URLDecoder.decode(entry.arguments!!.getString("book")!!, "UTF-8")
            val chapter = URLDecoder.decode(entry.arguments!!.getString("chapter")!!, "UTF-8")
            val page = entry.arguments!!.getInt("page")

            val readerVm: ReaderViewModel = viewModel(viewModelStoreOwner = entry)

            ReaderScreen(
                book = book,
                chapter = chapter,
                page = page,
                vm = readerVm,
                onBack = { navController.popBackStack() },
                onOpenMap = { bName, cName, mapFile ->
                    val mf = URLEncoder.encode(mapFile, "UTF-8")
                    val b = URLEncoder.encode(bName, "UTF-8")
                    val c = URLEncoder.encode(cName, "UTF-8")
                    navController.navigate("map/$b/$c/$mf")
                }
            )
        }

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

            val vm: MapInteractionViewModel = viewModel(viewModelStoreOwner = entry)

            MapInteractionScreen(
                book = book,
                chapter = chapter,
                mapJsonFile = mapfile,
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
