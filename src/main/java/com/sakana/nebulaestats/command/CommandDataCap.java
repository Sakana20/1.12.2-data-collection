package com.sakana.nebulaestats.command;

import com.sakana.nebulaestats.capture.CaptureRecorder;
import com.sakana.nebulaestats.capture.CaptureStatus;
import com.sakana.nebulaestats.capture.ResetResult;
import com.sakana.nebulaestats.capture.StopResult;
import com.sakana.nebulaestats.DataCollectionMod;
import com.sakana.nebulaestats.exit.ExitRegion;
import com.sakana.nebulaestats.exit.ExitRegionIds;
import com.sakana.nebulaestats.network.NetworkHandler;
import com.sakana.nebulaestats.selection.Selection;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CommandDataCap extends CommandBase {
    private final CaptureRecorder recorder;

    public CommandDataCap(CaptureRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public String getName() {
        return "ncs";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "\u7528\u6cd5\uff1a/ncs <start|stop|toggle|status|reset|exit>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            send(sender, getUsage(sender));
            return;
        }

        if ("start".equalsIgnoreCase(args[0])) {
            requireArgumentCount(sender, args, 1);
            executeStart(sender);
        } else if ("stop".equalsIgnoreCase(args[0])) {
            requireArgumentCount(sender, args, 1);
            executeStop(server, sender);
        } else if ("toggle".equalsIgnoreCase(args[0])) {
            requireArgumentCount(sender, args, 1);
            executeToggle(server, sender);
        } else if ("status".equalsIgnoreCase(args[0])) {
            requireArgumentCount(sender, args, 1);
            executeStatus(sender);
        } else if ("reset".equalsIgnoreCase(args[0])) {
            requireArgumentCount(sender, args, 1);
            executeReset(sender);
        } else if ("exit".equalsIgnoreCase(args[0])) {
            executeExit(server, sender, args);
        } else {
            send(sender, getUsage(sender));
        }
    }

    @Override
    public List<String> getTabCompletions(
            MinecraftServer server,
            ICommandSender sender,
            String[] args,
            @Nullable BlockPos targetPos
    ) {
        if (args.length == 1) {
            List<String> options = new ArrayList<String>();
            options.add("start");
            options.add("stop");
            options.add("toggle");
            options.add("status");
            options.add("reset");
            options.add("exit");
            return getListOfStringsMatchingLastWord(args, options);
        }

        if (args.length == 2 && "exit".equalsIgnoreCase(args[0])) {
            List<String> options = new ArrayList<String>();
            options.add("create");
            options.add("remove");
            options.add("list");
            options.add("info");
            options.add("gui");
            return getListOfStringsMatchingLastWord(args, options);
        }

        if (args.length == 3 && "exit".equalsIgnoreCase(args[0])
                && ("remove".equalsIgnoreCase(args[1]) || "info".equalsIgnoreCase(args[1]))) {
            List<String> ids = new ArrayList<String>();
            for (ExitRegion region : DataCollectionMod.EXIT_REGIONS.getAll()) {
                ids.add(region.getId());
            }
            return getListOfStringsMatchingLastWord(args, ids);
        }

        return Collections.emptyList();
    }

    private void executeStart(ICommandSender sender) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (recorder.start(player)) {
            send(sender, "\u5df2\u5f00\u59cb\u4e3a " + player.getName() + " \u5f55\u5236\u6570\u636e\u3002");
        } else {
            send(sender, "\u5f55\u5236\u5df2\u5728\u8fdb\u884c\u4e2d\u3002");
        }
    }

    private void executeStop(MinecraftServer server, ICommandSender sender) throws CommandException {
        try {
            StopResult result = recorder.stop(server);
            if (!result.wasRecordingActive()) {
                send(sender, "\u5f53\u524d\u6ca1\u6709\u6b63\u5728\u8fdb\u884c\u7684\u5f55\u5236\u3002");
                return;
            }

            send(sender, String.format(
                    "\u5df2\u4fdd\u5b58 %d \u4e2a\u6837\u672c\u5230 %s\u3002\u65f6\u957f\uff1a%.3f \u79d2\uff0c\u6c34\u5e73\u91cc\u7a0b\uff1a%.3f\uff0c\u4e09\u7ef4\u91cc\u7a0b\uff1a%.3f\u3002",
                    result.getSampleCount(),
                    result.getOutputFile().getPath(),
                    result.getDurationSeconds(),
                    result.getTotalDistance2d(),
                    result.getTotalDistance3d()
            ));
        } catch (IOException exception) {
            throw new CommandException("\u4fdd\u5b58\u5f55\u5236\u6570\u636e\u5931\u8d25\uff1a" + exception.getMessage());
        }
    }

    private void executeToggle(MinecraftServer server, ICommandSender sender) throws CommandException {
        if (recorder.getStatus().isRecording()) {
            executeStop(server, sender);
        } else {
            executeStart(sender);
        }
    }

    private void executeStatus(ICommandSender sender) {
        CaptureStatus status = recorder.getStatus();
        if (!status.isRecording()) {
            send(sender, String.format(
                    "\u5f55\u5236\u5df2\u505c\u6b62\u3002\u7f13\u5b58\u6837\u672c\uff1a%d\uff0c\u6c34\u5e73\u91cc\u7a0b\uff1a%.3f\uff0c\u4e09\u7ef4\u91cc\u7a0b\uff1a%.3f\u3002",
                    status.getSampleCount(),
                    status.getTotalDistance2d(),
                    status.getTotalDistance3d()
            ));
            return;
        }

        send(sender, String.format(
                "\u6b63\u5728\u4e3a %s \u5f55\u5236\u3002\u6837\u672c\uff1a%d\uff0c\u65f6\u957f\uff1a%.3f \u79d2\uff0c\u6c34\u5e73\u91cc\u7a0b\uff1a%.3f\uff0c\u4e09\u7ef4\u91cc\u7a0b\uff1a%.3f\u3002",
                status.getPlayerName(),
                status.getSampleCount(),
                status.getDurationSeconds(),
                status.getTotalDistance2d(),
                status.getTotalDistance3d()
        ));
    }

    private void executeReset(ICommandSender sender) {
        ResetResult result = recorder.reset();
        send(sender, "\u5f55\u5236\u7f13\u5b58\u5df2\u91cd\u7f6e\uff0c\u5df2\u79fb\u9664 " + result.getRemovedSampleCount() + " \u4e2a\u7f13\u5b58\u6837\u672c\u3002");
    }

    private void executeExit(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            send(sender, "\u7528\u6cd5\uff1a/ncs exit <create|remove|list|info|gui>");
            return;
        }

        if ("create".equalsIgnoreCase(args[1])) {
            executeExitCreate(server, sender, args);
        } else if ("remove".equalsIgnoreCase(args[1])) {
            executeExitRemove(server, sender, args);
        } else if ("list".equalsIgnoreCase(args[1])) {
            requireArgumentCount(sender, args, 2);
            executeExitList(sender);
        } else if ("info".equalsIgnoreCase(args[1])) {
            executeExitInfo(sender, args);
        } else if ("gui".equalsIgnoreCase(args[1])) {
            requireArgumentCount(sender, args, 2);
            NetworkHandler.openExitRegionGui(getCommandSenderAsPlayer(sender));
        } else {
            send(sender, "\u7528\u6cd5\uff1a/ncs exit <create|remove|list|info|gui>");
        }
    }

    private void executeExitCreate(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        requireArgumentCount(sender, args, 3);
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        String id = args[2];
        validateRegionId(id);

        Selection selection = DataCollectionMod.SELECTIONS.getSelection(player.getUniqueID());
        if (selection == null || !selection.isComplete()) {
            throw new CommandException("\u8bf7\u5148\u5de6\u952e\u9009\u62e9 pos1\uff0c\u518d\u53f3\u952e\u65b9\u5757\u9009\u62e9 pos2\u3002");
        }

        ExitRegion region = new ExitRegion(id, selection.getPos1(), selection.getPos2());
        if (!DataCollectionMod.EXIT_REGIONS.add(region)) {
            throw new CommandException("\u51fa\u53e3\u533a\u57df\u5df2\u5b58\u5728\uff1a" + id);
        }

        saveExitRegions(server);

        send(sender, "\u5df2\u521b\u5efa\u51fa\u53e3\u533a\u57df " + id + "\uff0c\u8303\u56f4\uff1a"
                + format(region.getMin()) + " \u81f3 " + format(region.getMax()) + "\u3002");
    }

    private void executeExitRemove(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        requireArgumentCount(sender, args, 3);
        String id = args[2];
        if (DataCollectionMod.EXIT_REGIONS.remove(id)) {
            saveExitRegions(server);
            send(sender, "\u5df2\u5220\u9664\u51fa\u53e3\u533a\u57df " + id + "\u3002");
        } else {
            throw new CommandException("\u672a\u627e\u5230\u51fa\u53e3\u533a\u57df\uff1a" + id);
        }
    }

    private void executeExitList(ICommandSender sender) {
        Collection<ExitRegion> regions = DataCollectionMod.EXIT_REGIONS.getAll();
        if (regions.isEmpty()) {
            send(sender, "\u5c1a\u672a\u5b9a\u4e49\u51fa\u53e3\u533a\u57df\u3002");
            return;
        }

        StringBuilder builder = new StringBuilder("\u5df2\u5b9a\u4e49\u51fa\u53e3\u533a\u57df\uff1a");
        for (ExitRegion region : regions) {
            builder.append(" ").append(region.getId());
        }
        send(sender, builder.toString());
    }

    private void executeExitInfo(ICommandSender sender, String[] args) throws CommandException {
        requireArgumentCount(sender, args, 3);
        String id = args[2];
        ExitRegion region = DataCollectionMod.EXIT_REGIONS.get(id);
        if (region == null) {
            throw new CommandException("\u672a\u627e\u5230\u51fa\u53e3\u533a\u57df\uff1a" + id);
        }

        send(sender, "\u51fa\u53e3\u533a\u57df " + id + "\uff1amin " + format(region.getMin())
                + "\uff0cmax " + format(region.getMax()) + "\u3002");
    }

    private void requireArgumentCount(ICommandSender sender, String[] args, int expected) throws CommandException {
        if (args.length != expected) {
            throw new CommandException(getUsage(sender));
        }
    }

    private void validateRegionId(String id) throws CommandException {
        if (!ExitRegionIds.isValid(id)) {
            throw new CommandException(ExitRegionIds.ID_RULE_MESSAGE);
        }
    }

    private String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private void saveExitRegions(MinecraftServer server) throws CommandException {
        try {
            DataCollectionMod.EXIT_REGION_STORAGE.save(server);
        } catch (IOException exception) {
            throw new CommandException("\u4fdd\u5b58\u51fa\u53e3\u533a\u57df\u5931\u8d25\uff1a" + exception.getMessage());
        }
    }

    private void send(ICommandSender sender, String message) {
        sender.sendMessage(new TextComponentString(message));
    }
}

