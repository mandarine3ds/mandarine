// Copyright 2025 Citra Project / Mandarine Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.adapters

import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.widget.PopupMenu
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.mandarine3ds.mandarine.HomeNavigationDirections
import io.github.mandarine3ds.mandarine.MandarineApplication
import io.github.mandarine3ds.mandarine.R
import io.github.mandarine3ds.mandarine.databinding.CardGameBinding
import io.github.mandarine3ds.mandarine.databinding.DialogAboutGameBinding
import io.github.mandarine3ds.mandarine.databinding.DialogShortcutBinding
import io.github.mandarine3ds.mandarine.features.cheats.ui.CheatsFragmentDirections
import io.github.mandarine3ds.mandarine.fragments.IndeterminateProgressDialogFragment
import io.github.mandarine3ds.mandarine.model.Game
import io.github.mandarine3ds.mandarine.utils.FileUtil
import io.github.mandarine3ds.mandarine.utils.GameIconUtils
import androidx.viewbinding.ViewBinding
import io.github.mandarine3ds.mandarine.databinding.CardGameBigBinding
import io.github.mandarine3ds.mandarine.utils.PlayTimeTracker
import io.github.mandarine3ds.mandarine.viewmodel.GamesViewModel
import io.github.mandarine3ds.mandarine.model.GameListItem

class GameAdapter(
    private val activity: AppCompatActivity,
    private val inflater: LayoutInflater,
    private val openImageLauncher: ActivityResultLauncher<String>?,
    private val filerGamesCallBack: ((Int, Int) -> Unit)? = null
) :
    ListAdapter<GameListItem, RecyclerView.ViewHolder>(AsyncDifferConfig.Builder(DiffCallback()).build()),
    View.OnClickListener, View.OnLongClickListener {
    private var lastClickTime = 0L
    private var imagePath: String? = null
    private var dialogShortcutBinding: DialogShortcutBinding? = null

    companion object {
        const val VIEW_TYPE_LIST = 0
        const val VIEW_TYPE_GRID = 1
        const val SEPARATOR = 2
    }

    private var viewType = VIEW_TYPE_LIST

    fun setViewType(type: Int) {
        viewType = type
        notifyDataSetChanged()
    }


    fun getViewType(): Int = viewType

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is GameListItem.GameItem -> viewType
        is GameListItem.Separator -> SEPARATOR
    }

    fun handleImageResult(uri: Uri?) {
        val path = uri?.toString()
        if (path != null) {
            imagePath = path
            refreshDialogIcon()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            SEPARATOR -> SeparatorViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.list_item_separator, parent, false)
            )
            else -> GameViewHolder(
                when (viewType) {
                    VIEW_TYPE_LIST -> CardGameBinding.inflate(inflater, parent, false)
                    VIEW_TYPE_GRID -> CardGameBigBinding.inflate(inflater, parent, false)
                    else -> CardGameBigBinding.inflate(inflater, parent, false)
                },
                viewType
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is GameListItem.GameItem -> (holder as GameViewHolder).bind(item.game)
            is GameListItem.Separator -> { }
        }
    }

    override fun getItemCount(): Int = currentList.size

    /**
     * Launches the game that was clicked on.
     *
     * @param view The card representing the game the user wants to play.
     */
    override fun onClick(view: View) {
        // Double-click prevention, using threshold of 1000 ms
        if (SystemClock.elapsedRealtime() - lastClickTime < 1000) {
            return
        }
        lastClickTime = SystemClock.elapsedRealtime()

        val holder = view.tag as GameViewHolder
        gameExists(holder)

        val preferences =
            PreferenceManager.getDefaultSharedPreferences(MandarineApplication.appContext)
        preferences.edit()
            .putLong(
                holder.game.keyLastPlayedTime,
                System.currentTimeMillis()
            )
            .apply()

        val action = HomeNavigationDirections.actionGlobalEmulationActivity(holder.game)
        view.findNavController().navigate(action)
    }

    /**
     * Opens the about game dialog for the game that was clicked on.
     *
     * @param view The view representing the game the user wants to play.
     */
    override fun onLongClick(view: View): Boolean {
        val context = view.context
        val holder = view.tag as GameViewHolder
        gameExists(holder)

        if (holder.game.titleId == 0L) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.properties)
                .setMessage(R.string.properties_not_loaded)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        } else {
            showAboutGameDialog(context, holder.game, holder, view)
        }
        return true
    }

    // Triggers a library refresh if the user clicks on stale data
    private fun gameExists(holder: GameViewHolder): Boolean {
        if (holder.game.isInstalled) {
            return true
        }

        val gameExists = DocumentFile.fromSingleUri(
            MandarineApplication.appContext,
            Uri.parse(holder.game.path)
        )?.exists() == true
        return if (!gameExists) {
            Toast.makeText(
                MandarineApplication.appContext,
                R.string.loader_error_file_not_found,
                Toast.LENGTH_LONG
            ).show()

            ViewModelProvider(activity)[GamesViewModel::class.java].reloadGames(true)
            false
        } else {
            true
        }
    }

    inner class GameViewHolder(
        private val binding: ViewBinding,
        private val viewType: Int
    ) : RecyclerView.ViewHolder(binding.root) {
        lateinit var game: Game

        init {
            binding.root.tag = this
            binding.root.setOnClickListener(this@GameAdapter)
            binding.root.setOnLongClickListener(this@GameAdapter)
        }

        fun bind(game: Game) {
            this.game = game

            when (viewType) {
                VIEW_TYPE_LIST -> bindListView(binding as CardGameBinding, game)
                VIEW_TYPE_GRID -> bindGridView(binding as CardGameBigBinding, game)
            }
        }

        private fun bindListView(binding: CardGameBinding, game: Game) {
            binding.gameTitle.text = game.title

            binding.imageGameScreen.scaleType = ImageView.ScaleType.CENTER_CROP
            GameIconUtils.loadGameIcon(activity, game, binding.imageGameScreen)

            binding.gameTitle.visibility = if (game.title.isEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }
            binding.gameRegion.visibility = if (game.company.isEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }

            val preferences = PreferenceManager.getDefaultSharedPreferences(binding.root.context)
            val isFavorite = game.keyIsFavorite.let {
                preferences.getBoolean(it, false)
            }

            binding.favoriteIcon.setImageResource(R.drawable.ic_star)

            binding.gameTitle.text = game.title
            binding.favoriteIcon.visibility = if (isFavorite) View.VISIBLE else View.GONE
            binding.gameRegion.text = game.regions
            binding.filename.text = game.filename

            val backgroundColorId =
                if (
                    isValidGame(game.filename.substring(game.filename.lastIndexOf(".") + 1).lowercase())
                ) {
                    R.attr.colorSurface
                } else {
                    R.attr.colorErrorContainer
                }
            binding.cardContents.setBackgroundColor(
                MaterialColors.getColor(
                    binding.cardContents,
                    backgroundColorId
                )
            )

            binding.gameTitle.postDelayed(
                {
                    binding.gameTitle.ellipsize = TextUtils.TruncateAt.MARQUEE
                    binding.gameTitle.isSelected = true

                    binding.gameRegion.ellipsize = TextUtils.TruncateAt.MARQUEE
                    binding.gameRegion.isSelected = true

                    binding.filename.ellipsize = TextUtils.TruncateAt.MARQUEE
                    binding.filename.isSelected = true
                },
                3000
            )
        }
    }

    inner class SeparatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private fun bindGridView(binding: CardGameBigBinding, game: Game) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(binding.root.context)
        val isFavorite = game.keyIsFavorite.let {
            preferences.getBoolean(it, false)
        }

        binding.favoriteIcon.setImageResource(R.drawable.ic_star)


        binding.textGameTitle.text = game.title
        binding.textGameTitle.visibility = if (game.title.isEmpty()) {
            View.GONE
        } else {
            View.VISIBLE
        }
        binding.favoriteIcon.visibility = if (isFavorite) View.VISIBLE else View.GONE
        GameIconUtils.loadGameIcon(activity, game, binding.imageGameScreen)
        binding.textGameTitle.postDelayed({
            binding.textGameTitle.ellipsize = TextUtils.TruncateAt.MARQUEE
            binding.textGameTitle.isSelected = true }, 3000)
    }

    private data class GameDirectories(
        val gameDir: String,
        val saveDir: String,
        val modsDir: String,
        val texturesDir: String,
        val appDir: String,
        val dlcDir: String,
        val updatesDir: String,
        val extraDir: String
    )
    private fun getGameDirectories(game: Game): GameDirectories {
        return GameDirectories(
            gameDir = game.path.substringBeforeLast("/"),
            saveDir = "sdmc/Nintendo 3DS/00000000000000000000000000000000/00000000000000000000000000000000/title/${String.format("%016x", game.titleId).lowercase().substring(0, 8)}/${String.format("%016x", game.titleId).lowercase().substring(8)}/data/00000001",
            modsDir = "load/mods/${String.format("%016X", game.titleId)}",
            texturesDir = "load/textures/${String.format("%016X", game.titleId)}",
            appDir = game.path.substringBeforeLast("/").split("/").filter { it.isNotEmpty() }.joinToString("/"),
            dlcDir = "sdmc/Nintendo 3DS/00000000000000000000000000000000/00000000000000000000000000000000/title/0004008c/${String.format("%016x", game.titleId).lowercase().substring(8)}/content",
            updatesDir = "sdmc/Nintendo 3DS/00000000000000000000000000000000/00000000000000000000000000000000/title/0004000e/${String.format("%016x", game.titleId).lowercase().substring(8)}/content",
            extraDir = "sdmc/Nintendo 3DS/00000000000000000000000000000000/00000000000000000000000000000000/extdata/00000000/${String.format("%016X", game.titleId).substring(8, 14).padStart(8, '0')}"
        )
    }

    private fun showOpenContextMenu(view: View, game: Game) {
        val dirs = getGameDirectories(game)

        val popup = PopupMenu(view.context, view).apply {
            menuInflater.inflate(R.menu.game_context_menu_open, menu)
            listOf(
                R.id.game_context_open_app to dirs.appDir,
                R.id.game_context_open_save_dir to dirs.saveDir,
                R.id.game_context_open_dlc to dirs.dlcDir,
                R.id.game_context_open_updates to dirs.updatesDir
            ).forEach { (id, dir) ->
                menu.findItem(id)?.isEnabled =
                    MandarineApplication.documentsTree.folderUriHelper(dir)?.let {
                        DocumentFile.fromTreeUri(view.context, it)?.exists()
                    } ?: false
            }
            menu.findItem(R.id.game_context_open_extra)?.let { item ->
                if (MandarineApplication.documentsTree.folderUriHelper(dirs.extraDir)?.let {
                        DocumentFile.fromTreeUri(view.context, it)?.exists()
                    } != true) {
                    menu.removeItem(item.itemId)
                }
            }
        }

        popup.setOnMenuItemClickListener { menuItem ->
            val intent = Intent(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setType("*/*")

            val uri = when (menuItem.itemId) {
                R.id.game_context_open_app -> MandarineApplication.documentsTree.folderUriHelper(dirs.appDir)
                R.id.game_context_open_save_dir -> MandarineApplication.documentsTree.folderUriHelper(dirs.saveDir)
                R.id.game_context_open_dlc -> MandarineApplication.documentsTree.folderUriHelper(dirs.dlcDir)
                R.id.game_context_open_textures -> MandarineApplication.documentsTree.folderUriHelper(dirs.texturesDir, true)
                R.id.game_context_open_mods -> MandarineApplication.documentsTree.folderUriHelper(dirs.modsDir, true)
                R.id.game_context_open_extra -> MandarineApplication.documentsTree.folderUriHelper(dirs.extraDir)
                else -> null
            }

            uri?.let {
                intent.data = it
                view.context.startActivity(intent)
                true
            } ?: false
        }

        popup.show()
    }

    private fun showUninstallContextMenu(view: View, game: Game, bottomSheetDialog: BottomSheetDialog) {
        val dirs = getGameDirectories(game)
        val popup = PopupMenu(view.context, view).apply {
            menuInflater.inflate(R.menu.game_context_menu_uninstall, menu)
            listOf(
                R.id.game_context_uninstall to dirs.gameDir,
                R.id.game_context_uninstall_dlc to dirs.dlcDir,
                R.id.game_context_uninstall_updates to dirs.updatesDir,
                R.id.game_context_delete_playtime to ""

            ).forEach { (id, dir) ->
                if (id == R.id.game_context_delete_playtime) {
                    menu.findItem(id)?.isEnabled =
                        PlayTimeTracker.getPlayTime(game.titleId) != "0s"
                    menu.findItem(id)?.setOnMenuItemClickListener {
                        PlayTimeTracker.deletePlayTime(game.titleId)
                        ViewModelProvider(activity)[GamesViewModel::class.java].reloadGames(true)
                        bottomSheetDialog.dismiss()
                        true
                    }
                } else {
                    menu.findItem(id)?.isEnabled =
                        MandarineApplication.documentsTree.folderUriHelper(dir)?.let {
                            DocumentFile.fromTreeUri(view.context, it)?.exists()
                        } ?: false
                }
            }
        }

        popup.setOnMenuItemClickListener { menuItem ->
            val uninstallAction: () -> Unit = {
                when (menuItem.itemId) {
                    R.id.game_context_uninstall -> MandarineApplication.documentsTree.deleteDocument(dirs.gameDir)
                    R.id.game_context_uninstall_dlc -> FileUtil.deleteDocument(MandarineApplication.documentsTree.folderUriHelper(dirs.dlcDir)
                        .toString())
                    R.id.game_context_uninstall_updates -> FileUtil.deleteDocument(MandarineApplication.documentsTree.folderUriHelper(dirs.updatesDir)
                        .toString())
                }
                ViewModelProvider(activity)[GamesViewModel::class.java].reloadGames(true)
                bottomSheetDialog.dismiss()
            }

            if (menuItem.itemId in listOf(R.id.game_context_uninstall, R.id.game_context_uninstall_dlc, R.id.game_context_uninstall_updates)) {
                IndeterminateProgressDialogFragment.newInstance(activity, R.string.uninstalling, false, uninstallAction)
                    .show(activity.supportFragmentManager, IndeterminateProgressDialogFragment.TAG)
                true
            } else {
                false
            }
        }

        popup.show()
    }

    private fun showAboutGameDialog(context: Context, game: Game, holder: GameViewHolder, view: View) {
        val binding = DialogAboutGameBinding.inflate(activity.layoutInflater)

        val bottomSheetDialog = BottomSheetDialog(context)
        bottomSheetDialog.setContentView(binding.root)

        binding.aboutGameTitle.text = game.title
        binding.aboutGameCompany.text = game.company
        binding.aboutGameId.text = String.format("ID: %016X", game.titleId)
        GameIconUtils.loadGameIcon(activity, game, binding.gameIcon)

        binding.aboutGamePlay.setOnClickListener {
            val action = HomeNavigationDirections.actionGlobalEmulationActivity(holder.game)
            view.findNavController().navigate(action)
            bottomSheetDialog.dismiss()
        }

        binding.favoriteGame.apply {
            val isFavorite = holder.game.keyIsFavorite.let {
                PreferenceManager.getDefaultSharedPreferences(context).getBoolean(it, false)
            }
            binding.favoriteGame.setIconResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_frame)

            binding.favoriteGame.setOnClickListener {
                val newFavoriteState = !isFavorite
                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                preferences.edit()
                    .putBoolean(holder.game.keyIsFavorite, newFavoriteState)
                    .apply()
                binding.favoriteGame.setIconResource(if (newFavoriteState) R.drawable.ic_star else R.drawable.ic_star_frame)

                val position = currentList.indexOf(GameListItem.GameItem(game))

                filerGamesCallBack?.invoke(position, if (newFavoriteState) 1 else -1)

                if (position != -1) {
                    notifyItemChanged(position)
                }

                bottomSheetDialog.dismiss()
            }
        }


        binding.gameShortcut.setOnClickListener {
            fun showShortcutDialog(game: Game) {
            dialogShortcutBinding = DialogShortcutBinding.inflate(activity.layoutInflater)

            dialogShortcutBinding!!.shortcutNameInput.setText(game.title)
            GameIconUtils.loadGameIcon(activity, game, dialogShortcutBinding!!.shortcutIcon)

            dialogShortcutBinding!!.shortcutIcon.setOnClickListener {
                openImageLauncher?.launch("image/*")
            }

            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.create_shortcut)
                .setView(dialogShortcutBinding!!.root)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                val shortcutName = dialogShortcutBinding!!.shortcutNameInput.text.toString()
                if (shortcutName.isEmpty()) {
                    Toast.makeText(context, R.string.shortcut_name_empty, Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val iconBitmap = (dialogShortcutBinding!!.shortcutIcon.drawable as BitmapDrawable).bitmap
                val shortcutManager = activity.getSystemService(ShortcutManager::class.java)

                CoroutineScope(Dispatchers.IO).launch {
                    val icon = Icon.createWithBitmap(iconBitmap)
                    val shortcut = ShortcutInfo.Builder(context, shortcutName)
                    .setShortLabel(shortcutName)
                    .setIcon(icon)
                    .setIntent(game.launchIntent.apply {
                        putExtra("launchedFromShortcut", true)
                    })
                    .build()

                    shortcutManager?.requestPinShortcut(shortcut, null)
                }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            }

            showShortcutDialog(game)
            bottomSheetDialog.dismiss()
        }

        binding.menuButtonOpen.setOnClickListener {
            showOpenContextMenu(it, game)
        }

        binding.menuButtonUninstall.setOnClickListener {
            showUninstallContextMenu(it, game, bottomSheetDialog)
        }

        binding.cheats.setOnClickListener {
            val action = CheatsFragmentDirections.actionGlobalCheatsFragment(holder.game.titleId)
            view.findNavController().navigate(action)
            bottomSheetDialog.dismiss()
        }

        binding.aboutGamePlaytime.text =
            "Playtime: " + PlayTimeTracker.getPlayTime(game.titleId)

        val bottomSheetBehavior = bottomSheetDialog.getBehavior()
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        bottomSheetDialog.show()
    }

    private fun refreshDialogIcon() {
        if (imagePath != null) {
            val originalBitmap = BitmapFactory.decodeStream(
                MandarineApplication.appContext.contentResolver.openInputStream(
                    Uri.parse(imagePath)
                )
            )
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 108, 108, true)
            dialogShortcutBinding?.shortcutIcon?.setImageBitmap(scaledBitmap)
        }
    }

    private fun isValidGame(extension: String): Boolean {
        return Game.badExtensions.stream()
            .noneMatch { extension == it.lowercase() }
    }

    private class DiffCallback : DiffUtil.ItemCallback<GameListItem>() {
        override fun areItemsTheSame(oldItem: GameListItem, newItem: GameListItem): Boolean {
            return when {
                oldItem is GameListItem.GameItem && newItem is GameListItem.GameItem ->
                    oldItem.game.titleId == newItem.game.titleId
                oldItem is GameListItem.Separator && newItem is GameListItem.Separator -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: GameListItem, newItem: GameListItem): Boolean {
            return when {
                oldItem is GameListItem.GameItem && newItem is GameListItem.GameItem ->
                    oldItem.game == newItem.game
                oldItem is GameListItem.Separator && newItem is GameListItem.Separator -> true
                else -> false
            }
        }
    }
}
