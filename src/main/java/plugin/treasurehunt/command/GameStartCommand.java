package plugin.treasurehunt.command;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import plugin.treasurehunt.data.ChestDetail;
import plugin.treasurehunt.data.GameLocationDate;
import plugin.treasurehunt.PlayerScoreData;
import plugin.treasurehunt.data.ExecutingPlayer;
import plugin.treasurehunt.mapper.data.PlayerScore;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class GameStartCommand extends BaseCommand implements Listener {
    public static boolean hasChest;
    public boolean hasDia;
    public static long gameTimeCounter;
    public String difficulty;
    public static final String EASY ="easy";
    public static final String NORMAL ="normal";
    public static final String HARD ="hard";
    public static final String NONE ="none";
    public static final String LIST ="list";
    public static final String RULE ="rule";
    private PlayerScoreData playerScoreData = new PlayerScoreData();
    public static ExecutingPlayer nowExecutingPlayer = new ExecutingPlayer();

    @Override
    public boolean onExecutePlayerCommand(Player player, @NotNull Command command, @NotNull String label, String[] args) {
        if(args.length == 1 && LIST.equals(args[0])){
            sendPlayerScoreList(player);
            return false;
        }else if(args.length == 1 && RULE.equals(args[0])){
            sendGameRule(player);
            return false;
        }
        difficulty = getDifficulty(player,args);
        if (difficulty.equals(NONE)) return false;
        hasFlag();
        scoreReset();
        startCountdown(player);
        gameplay(player, difficulty);
        return true;
    }

    /**
     * コマンドの引数から難易度を取得
     * @param player　プレイヤー
     * @param args　コマンドの引数
     * @return　難易度
     */
    private String getDifficulty(Player player, @NotNull String[] args) {
        if(args.length == 1 && (EASY.equals(args[0]) || NORMAL.equals(args[0]) || HARD.equals(args[0]))){
            return args[0];
        }
        player.sendMessage("コマンドと半角スペースに続いて、難易度を入力してください(easy,normal,hard)");
        return NONE;
    }

    /**
     * ゲーム開始（チェストの出現）と、ゲーム終了（チェストからダイヤがなくなる）のフラグ用
     */
    private void hasFlag() {
        this.hasChest = true;
        this.hasDia = true;
    }

    /**
     * スコアのリセット
     */
    public void scoreReset() {
        nowExecutingPlayer = new ExecutingPlayer();
    }

    /**
     * ゲームスタート時のカウントダウン
     * @param player　プレイヤー
     */
    private void startCountdown(Player player) {
        int countdownTime = 5;
        for (int i = countdownTime; i > 0; i--) {
            player.sendTitle(String.valueOf(i),"ゲーム開始位置についてください");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        player.sendTitle("GO!!","ゲームスタート！");
    }

    /**
     * ゲームの実行と終了。周囲にチェストが現れ、鉱石がランダムで格納される。
     * ダイヤをゲットするとゲーム終了の処理をする。
     * @param player　プレイヤー
     * @param difficulty　ゲームの難易度
     */
    private void gameplay(Player player,String difficulty){
        //ゲーム終了
        if(!hasDia){
            int finishedTime = (int) ((System.currentTimeMillis() - gameTimeCounter) / 1000);
                if(finishedTime <= 10){
                    nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() +500);
                }else if(finishedTime <= 20){
                nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() +100);
                }
            player.sendTitle("ゲームクリア！",
                    "あなたのスコアは"
                            + nowExecutingPlayer.getPoint()
                            + "点！",10,80,10);
            player.sendMessage(ChatColor.GREEN + "ダイヤを見つけた！\n"
                            + difficulty + "モードを"
                            + finishedTime +  "秒でクリア！");
            playerScoreData.insert(
                new PlayerScore(nowExecutingPlayer.getPoint()
                    ,finishedTime
                    ,difficulty));
            return;
        }
        //ゲーム開始
        gameTimeCounter = System.currentTimeMillis();/*時間計測開始*/
        GameLocationDate.chestLocation(player);/*ゲームエリア生成*/
        ChestDetail.arrangementChest(player);/*チェスト配置*/
        ChestDetail.putOreRandom(difficulty);/*鉱石格納*/
    }

    /**
     *鉱石別にポイント加算（ゲーム中、ゲームエリア内のチェストインベントリをクリックした場合）
     * @param e チェストインベントリをクリック
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        //　生成したチェスト以外は対象外
        Player player = (Player) e.getWhoClicked();
        if (ChestDetail.chestLocationList == null ||
                e.getCurrentItem() == null ||
                !ChestDetail.chestLocationList.contains(e.getInventory().getLocation())) {
            return;
        }
        // チェストにアイテムを入れるのを禁止
        if (Objects.requireNonNull(
                e.getClickedInventory()).getType() == InventoryType.PLAYER) {
            player.sendMessage("アイテムを入れるのは禁止です");
            e.setCancelled(true);
        }
        // クリックされたアイテムがチェスト内のものだったらポイント加算
        if (e.getClickedInventory().getType() == InventoryType.CHEST) {
            Material itemType = e.getCurrentItem().getType();
            int pointAdd = 0;
            String message = "";
            String bonusMessage = "";
            switch (itemType) {
                case DIAMOND:
                    ChestDetail.chestReset(player);
                    nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() + 100);
                    this.hasDia = false;
                    gameplay(player, difficulty);
                    break;
                case EMERALD:
                    pointAdd = 50;
                    message = "やった！エメラルドをゲット！";
                    break;
                case LAPIS_LAZULI:
                    pointAdd = 20;
                    message = "ラピスラズリをゲット！";
                    break;
                case COAL:
                    pointAdd = 10;
                    message = "石炭をゲット！";
                    break;
                case BONE:
                    pointAdd = -5;
                    message = "残念、骨だった・・・";
                    break;
                case GOLDEN_APPLE:
                    long getApple = gameTimeCounter;
                        if ((int) ((System.currentTimeMillis() - getApple) / 1000) <= 10) {
                            nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() + 500);
                            bonusMessage = "タイムボーナス500点！";
                        }
                    pointAdd = 100;
                    message = "やった！金のりんごをゲット！";
                    break;
            }
                    if (hasDia == true) {
                        nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() + pointAdd);
                        player.sendMessage(message);
                        player.sendMessage(ChatColor.YELLOW + bonusMessage);
                        player.sendMessage("現在のポイントは"
                                + nowExecutingPlayer.getPoint() + "点！");
                    }
                    //チェストのアイテムはクリックでエアーに
                    e.getClickedInventory().setItem(e.getSlot(), new ItemStack(Material.AIR));
            }
        }

    /**
     * プレイヤーがゲームエリア外に出た場合、ゲームを終了する
     * @param e プレイヤーが移動をする
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (!GameLocationDate.playerInGameArea(player) && hasChest) {
            player.sendMessage("エリア外にでました、ゲームを終了します");
            ChestDetail.chestReset(player);
        }
    }

    /**
     * ゲーム終了（ダイヤゲット）まで、チェストの破壊を禁止（Creative以外）
     * @param e　チェストに攻撃
     */
    @EventHandler
    public void onBlockDamage(BlockDamageEvent e) {
        if(hasDia &&
            ChestDetail.chestLocationList .contains(e.getBlock().getLocation())){
            e.setCancelled(true);
        }
    }

    /**
     * チェストが壊されたとき、ゲームを終了（Creativeモード）
     * @param e　チェストを壊したとき
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        if (ChestDetail.chestLocationList  != null &&
            block.getType() == Material.CHEST &&
            hasChest &&
            ChestDetail.chestLocationList .contains(e.getBlock().getLocation())) {
            player.sendMessage("チェストが破壊されました、ゲームを終了します");
            ChestDetail.chestReset(player);
        }
    }

    /**
     * 中身を取らずチェストをしめると、骨は減点増（減点回避のペナルティ）・ダイヤは加点なしでゲーム終了（中身の確認のみを回避）
     * @param e　チェストをしめる
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Chest chest && hasChest) {
            Inventory chestInventory = chest.getInventory();
            Player player = (Player) e.getPlayer();

            for (ItemStack item : chestInventory.getContents()) {
                if (ChestDetail.chestLocationList != null && item != null && item.getType() == Material.BONE) {
                    nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() -20);
                    player.sendMessage(ChatColor.RED +"骨をスルー、ペナルティ-20点！");
                    player.sendMessage("現在のポイントは" + nowExecutingPlayer.getPoint() + "点！");

                }else if (ChestDetail.chestLocationList != null && item != null && item.getType() == Material.DIAMOND) {
                    player.sendMessage("ダイヤをそっと戻し、ダイヤ探しを中断した");
                    ChestDetail.chestReset(player);
                }
            }
        }
    }

    /*
    チェストの繋ぎ置き禁止
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Block placedBlock = e.getBlockPlaced();

        if (placedBlock.getState() instanceof Chest && GameLocationDate.playerInGameArea(player)) {
            e.setCancelled(true);
            player.sendMessage("ゲーム中はエリア内にチェストを置けません。");
        }
    }

    /**
     * ゲーム中にログアウトするとチェストをリセット（チェスト増殖対策）
     * @param e　プレイヤーがログアウト
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        ChestDetail.chestReset(player);
    }

    /**
     *登録されているスコアリストをメッセージに送る
     * @param player　プレイヤー
     */
    private void sendPlayerScoreList(Player player) {
        List<PlayerScore> playerScoreList = playerScoreData.selectList();
        for(PlayerScore playerScore : playerScoreList){
            player.sendMessage("｜"
                    + playerScore.getId() + "｜"
                    + playerScore.getDate() + "｜"
                    + playerScore.getScore() + "｜"
                    + playerScore.getTime() + "｜"
                    + playerScore.getDifficulty() + "｜");
        }
    }
    /**
     * ゲームのルール説明
     * @param player プレイヤー
     */
    private void sendGameRule(Player player) {
        player.sendMessage("このゲームはコマンド入力でチェストが配置されます\n" +
                "チェストの中の鉱石別でポイントが付与されます。\n" +
                "レベルは、easy、normal、hardがあります。\n" +
                "付与ポイントは、ダイヤ100点・エメラルド50点、\n" +
                "ラピスラズリ20点・石炭10点で、骨は-5点です。\n" +
                "ボーナスは、10秒以内クリアで500点、20秒以内クリアで100点、\n" +
                "10秒以内に金のリンゴを見つけると500点です。、\n" +
                "ダイヤを見つけるとゲーム終了になります。\n");
        player.sendMessage(ChatColor.GREEN +
                "・何もない場所でプレイしてください。\n" +
                "・チェストは、現在地から南方向に配置されます");
    }

    @Override
    public boolean onExecutePlayerNPCCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return false;
    }
}