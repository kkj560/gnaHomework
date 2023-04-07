package com.example.gnahomework

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


interface GameDataRepository {
    val games: MutableLiveData<List<GameData>>
    fun getGames(): LiveData<List<GameData>>
    fun handleProgress(packageName: String?, progress: Int)
    fun loadGames(context: Context)
}

@Singleton
class GameDataRepositoryImpl @Inject constructor(application: Application) : GameDataRepository {

    override val games: MutableLiveData<List<GameData>> = MutableLiveData()

    override fun getGames(): LiveData<List<GameData>> {
        return games
    }

    init {
        loadGames(application.applicationContext)
    }

    override fun handleProgress(packageName: String?, progress: Int) {
        // Send the message
        Timber.d("In handleProgress")
        val updatedList = games.value?.toMutableList()
        val index = updatedList?.indexOf(updatedList?.find { it.packageName == packageName })

        if (index != null && index != -1) {
            updatedList?.get(index)
                ?.let {
                    if(it.progress < progress && progress != 0)
                        updatedList?.set(index, it.copy(progress = progress))
                }
            games.postValue(updatedList)
        }
    }

    override fun loadGames(context: Context) {
        var jsonString: String = context.assets.open("game/game.json")
            .bufferedReader()
            .use { it.readText() }

        val listGameType = object : TypeToken<List<Game>>() {}.type
        val gameList: List<Game> = Gson().fromJson(jsonString, listGameType)

        games.postValue(gameList.map { GameData(it.appName, it.packageName, it.iconUri, -1) })
    }
}

@Module
@InstallIn(SingletonComponent::class)
object GameDataRepositoryModule {
    @Provides
    @Singleton
    fun provideGameDataRepository(repositoryImpl: GameDataRepositoryImpl): GameDataRepository {
        return repositoryImpl
    }
}

