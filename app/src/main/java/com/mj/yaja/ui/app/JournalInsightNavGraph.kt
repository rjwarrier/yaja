package com.mj.yaja.ui.app

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mj.yaja.ui.navigation.Route
import com.mj.yaja.ui.screens.KeywordDetailScreen
import com.mj.yaja.ui.screens.KeywordsScreen
import com.mj.yaja.ui.screens.LookbackScreen
import com.mj.yaja.ui.screens.ComplianceMasterScreen
import com.mj.yaja.ui.screens.ReviewPeriodType
import com.mj.yaja.ui.screens.ReviewScreen
import com.mj.yaja.ui.screens.StatisticsScreen
import com.mj.yaja.ui.screens.TodosScreen
import com.mj.yaja.ui.viewmodel.JournalViewModel

internal fun NavGraphBuilder.addInsightRoutes(
        navController: NavHostController,
        viewModel: JournalViewModel,
        onOpenDrawer: () -> Unit
) {
        composable(Route.Lookback.path) {
                LookbackScreen(
                        viewModel = viewModel,
                        onOpenDrawer = onOpenDrawer,
                        onNavigateToDate = { date ->
                                viewModel.selectDate(date)
                                navController.navigate(Route.Home.path) {
                                        popUpTo(Route.Home.path) { inclusive = true }
                                }
                        },
                        onSurpriseMeNavigate = { date ->
                                viewModel.selectDate(date)
                                navController.navigate(Route.Home.path)
                        },
                        onNavigateToReview = { period ->
                                navController.navigate(Route.Review.create(period.routeValue))
                        }
                )
        }
        composable(
                route = Route.Review.path,
                arguments = listOf(
                        navArgument(Route.Review.ARG_PERIOD) {
                                type = NavType.StringType
                        }
                )
        ) { backStackEntry ->
                val period =
                        ReviewPeriodType.fromRoute(
                                backStackEntry.arguments?.getString(Route.Review.ARG_PERIOD)
                        )
                ReviewScreen(
                        viewModel = viewModel,
                        period = period,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToDate = { date ->
                                viewModel.selectDate(date)
                                navController.navigate(Route.Home.path) {
                                        popUpTo(Route.Home.path) { inclusive = true }
                                }
                        }
                )
        }
        composable(Route.Statistics.path) {
                StatisticsScreen(
                        viewModel = viewModel,
                        onOpenDrawer = onOpenDrawer,
                        onNavigateBack = { navController.popBackStack() }
                )
        }
        composable(Route.Keywords.path) {
                KeywordsScreen(
                        viewModel = viewModel,
                        onOpenDrawer = onOpenDrawer,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToKeyword = { keywordId ->
                                navController.navigate(Route.KeywordDetail.create(keywordId))
                        }
                )
        }
        composable(
                route = Route.KeywordDetail.path,
                arguments = listOf(
                        navArgument(Route.KeywordDetail.ARG_KEYWORD_ID) {
                                type = NavType.StringType
                        }
                )
        ) { backStackEntry ->
                val keywordId = backStackEntry.arguments
                        ?.getString(Route.KeywordDetail.ARG_KEYWORD_ID)
                        ?: return@composable
                KeywordDetailScreen(
                        viewModel = viewModel,
                        keywordId = keywordId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToDate = { date ->
                                viewModel.selectDate(date)
                                navController.navigate(Route.Home.path) {
                                        popUpTo(Route.Home.path) { inclusive = true }
                                }
                        },
                        onNavigateToKeyword = { relatedKeywordId ->
                                if (relatedKeywordId != keywordId) {
                                        navController.navigate(Route.KeywordDetail.create(relatedKeywordId))
                                }
                        }
                )
        }
        composable(Route.Todos.path) {
                TodosScreen(
                        viewModel = viewModel,
                        onOpenDrawer = onOpenDrawer,
                        onNavigateToDate = { date ->
                                viewModel.selectDate(date)
                                navController.navigate(Route.Home.path)
                        },
                        onNavigateToComplianceMaster = {
                                navController.navigate(Route.ComplianceMaster.path)
                        }
                )
        }
        composable(Route.ComplianceMaster.path) {
                ComplianceMasterScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                )
        }
}
