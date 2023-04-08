package net.jandie1505.getblocks;

import org.bukkit.Axis;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GetBlocks extends JavaPlugin implements CommandExecutor, TabCompleter {

    @Override
    public void onEnable() {
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdirs();
        }

        this.getCommand("getblocks").setExecutor(this);
        this.getCommand("getblocks").setTabCompleter(this);
    }

    public JSONObject getBlockJSON(Block block) {
        JSONObject jsonData = new JSONObject();

        BlockData blockData = block.getBlockData();

        if (blockData instanceof Ageable) {
            jsonData.put("age", ((Ageable) blockData).getAge());
            jsonData.put("max_age", ((Ageable) blockData).getMaximumAge());
        }

        if (blockData instanceof AnaloguePowerable) {
            jsonData.put("power", ((AnaloguePowerable) blockData).getPower());
            jsonData.put("max_power", ((AnaloguePowerable) blockData).getMaximumPower());
        }

        if (blockData instanceof Attachable) {
            jsonData.put("attached", ((Attachable) blockData).isAttached());
        }

        if (blockData instanceof Bisected) {
            jsonData.put("half", ((Bisected) blockData).getHalf().toString());
        }

        if (blockData instanceof Directional) {
            jsonData.put("facing", ((Directional) blockData).getFacing().toString());

            JSONArray faces = new JSONArray();
            for (BlockFace face : Set.copyOf(((Directional) blockData).getFaces())) {
                faces.put(face.toString());
            }

            jsonData.put("faces", faces);
        }

        if (blockData instanceof FaceAttachable) {
            jsonData.put("face", ((FaceAttachable) blockData).getAttachedFace().toString());
        }

        if (blockData instanceof Hangable) {
            jsonData.put("hanging", ((Hangable) blockData).isHanging());
        }

        if (blockData instanceof Levelled) {
            jsonData.put("level", ((Levelled) blockData).getLevel());
            jsonData.put("max_level", ((Levelled) blockData).getMaximumLevel());
        }

        if (blockData instanceof Lightable) {
            jsonData.put("lit", ((Lightable) blockData).isLit());
        }

        if (blockData instanceof MultipleFacing) {
            JSONArray faces = new JSONArray();

            for (BlockFace face : Set.copyOf(((MultipleFacing) blockData).getFaces())) {
                faces.put(face.toString());
            }

            jsonData.put("multiple_faces", faces);

            JSONArray allowedFaces = new JSONArray();

            for (BlockFace face : Set.copyOf(((MultipleFacing) blockData).getAllowedFaces())) {
                allowedFaces.put(face.toString());
            }

            jsonData.put("multiple_faces_allowed", allowedFaces);
        }

        if (blockData instanceof Openable) {
            jsonData.put("open", ((Openable) blockData).isOpen());
        }

        if (blockData instanceof Orientable) {
            jsonData.put("axis", ((Orientable) blockData).getAxis());

            JSONArray axes = new JSONArray();

            for (Axis axis : Set.copyOf(((Orientable) blockData).getAxes())) {
                axes.put(axis.toString());
            }

            jsonData.put("axes", axes);
        }

        if (blockData instanceof Powerable) {
            jsonData.put("powered", ((Powerable) blockData).isPowered());
        }

        if (blockData instanceof Rail) {
            jsonData.put("shape", ((Rail) blockData).getShape().toString());

            JSONArray shapes = new JSONArray();

            for (Rail.Shape shape : Set.copyOf(((Rail) blockData).getShapes())) {
                shapes.put(shape.toString());
            }

            jsonData.put("shapes", shapes);
        }

        if (blockData instanceof Rotatable) {
            jsonData.put("rotation", ((Rotatable) blockData).getRotation().toString());
        }

        if (blockData instanceof Snowable) {
            jsonData.put("snowy", ((Snowable) blockData).isSnowy());
        }

        if (blockData instanceof  Waterlogged) {
            jsonData.put("waterlogged", ((Waterlogged) blockData).isWaterlogged());
        }

        JSONObject jsonBlock = new JSONObject();

        jsonBlock.put("pos_x", block.getX());
        jsonBlock.put("pos_y", block.getY());
        jsonBlock.put("pos_z", block.getZ());
        jsonBlock.put("type", block.getType().toString());
        jsonBlock.put("data", jsonData);

        return jsonBlock;
    }

    public JSONArray scanBlocks(World world, int startX, int startY, int startZ, int endX, int endY, int endZ) {
        JSONArray blocks = new JSONArray();

        this.getLogger().info("Scanning blocks from " + startX + " " + startY + " " + startZ + " to " + endX + " " + endY + " " + endZ);

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                for (int z = startZ; z <= endZ; z++) {

                    Chunk chunk = world.getChunkAt(x, z);
                    if (!chunk.isLoaded()) {
                        chunk.load(false);
                    }

                    blocks.put(this.getBlockJSON(world.getBlockAt(x, y, z)));

                }
            }
        }

        this.getLogger().info("Scan finished");

        return blocks;
    }

    private boolean writeJSONtoFile(JSONArray jsonArray, boolean format, String filename) {
        try {

            if (!filename.matches("[0-9a-zA-Z._-]+")) {
                return false;
            }

            if (!this.getDataFolder().exists()) {
                return false;
            }

            File file = new File(this.getDataFolder(), filename);

            if (!file.exists()) {

                FileWriter writer = new FileWriter(file);

                if (format) {
                    writer.write(jsonArray.toString(4));
                } else {
                    writer.write(jsonArray.toString());
                }

                writer.flush();
                writer.close();

                return true;

            } else {
                return false;
            }

        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission("getblocks.use")) {
            sender.sendMessage("§cNo permission");
            return true;
        }

        if (args.length != 9) {
            sender.sendMessage("§7Use §b/getblocks <world> <x1> <y1> <z1> <x2> <y2> <z2> <filename> <format> §7to export a part of the map");
            return true;
        }

        try {

            World world = this.getServer().getWorld(args[0]);
            int x1 = Integer.parseInt(args[1]);
            int y1 = Integer.parseInt(args[2]);
            int z1 = Integer.parseInt(args[3]);
            int x2 = Integer.parseInt(args[4]);
            int y2 = Integer.parseInt(args[5]);
            int z2 = Integer.parseInt(args[6]);
            String filename = args[7];
            boolean format = Boolean.parseBoolean(args[8]);

            if (world == null) {
                sender.sendMessage("§cThe world you have specified does not exist");
                return true;
            }

            if (!filename.matches("[0-9a-zA-Z._-]+")) {
                sender.sendMessage("§cThe filename contains invalid characters");
                return true;
            }

            File file = new File(this.getDataFolder(), filename);

            if (file.exists()) {
                sender.sendMessage("§cFile already exists");
                return true;
            }

            sender.sendMessage("§aStarting async task to scan blocks...");

            this.getServer().getScheduler().runTaskAsynchronously(this, task -> {

                this.getLogger().info("[" + task.getTaskId()+ "] " + " Scanning blocks");
                sender.sendMessage("§aScanning blocks. This can take some time...");

                JSONArray blocks = this.scanBlocks(world, x1, y1, z1, x2, y2, z2);

                this.getLogger().info("[" + task.getTaskId()+ "] " + " Scan finished. Writing file...");
                sender.sendMessage("§aScan finished. Writing to file...");

                if (this.writeJSONtoFile(blocks, format, filename)) {
                    this.getLogger().info("[" + task.getTaskId()+ "] " + " File successfully written");
                    sender.sendMessage("§aFile has been successfully written");
                } else {
                    this.getLogger().info("[" + task.getTaskId()+ "] " + " Error while writing file");
                    sender.sendMessage("§cError while writing file");
                }

            });

        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cPlease specify valid int values for the coordinates");
        }

        return true;
    }

    public Integer[] getPlayerTargetBlock(Player player) {
        if (player == null) {
            return new Integer[]{null, null, null};
        }

        Block block = player.getTargetBlockExact(5);

        if (block == null) {
            return new Integer[]{null, null, null};
        }

        return new Integer[]{block.getX(), block.getY(), block.getZ()};
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            return List.of();
        }

        Player player = (Player) sender;

        List<String> tab = new ArrayList<>();

        switch (args.length) {
            case 1:
                tab.add(player.getWorld().getName());
                break;
            case 2:
            case 5:
                Integer x1 = this.getPlayerTargetBlock(player)[0];
                if (x1 != null) {
                    tab.add(String.valueOf(x1));
                }
                break;
            case 3:
            case 6:
                Integer y1 = this.getPlayerTargetBlock(player)[1];
                if (y1 != null) {
                    tab.add(String.valueOf(y1));
                }
                break;
            case 4:
            case 7:
                Integer z1 = this.getPlayerTargetBlock(player)[2];
                if (z1 != null) {
                    tab.add(String.valueOf(z1));
                }
                break;
            case 9:
                tab.add("true");
                tab.add("false");
                break;
            default:
                break;
        }

        return tab;
    }

}
