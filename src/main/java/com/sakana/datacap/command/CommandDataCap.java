package com.sakana.datacap.command;

import com.sakana.datacap.capture.CaptureRecorder;
import com.sakana.datacap.capture.CaptureStatus;
import com.sakana.datacap.capture.ResetResult;
import com.sakana.datacap.capture.StopResult;
import com.sakana.datacap.DataCollectionMod;
import com.sakana.datacap.exit.ExitRegion;
import com.sakana.datacap.selection.Selection;
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
    private static final String ID_PATTERN = "[A-Za-z0-9_\\-:.]+";

    private final CaptureRecorder recorder;

    public CommandDataCap(CaptureRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public String getName() {
        return "datacap";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/datacap <start|stop|toggle|status|reset|exit>";
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
            executeExit(sender, args);
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
            send(sender, "Data capture started for " + player.getName() + ".");
        } else {
            send(sender, "Data capture is already running.");
        }
    }

    private void executeStop(MinecraftServer server, ICommandSender sender) throws CommandException {
        try {
            StopResult result = recorder.stop(server);
            if (!result.wasRecordingActive()) {
                send(sender, "Data capture is not running.");
                return;
            }

            send(sender, String.format(
                    "Saved %d samples to %s. Duration: %.3fs, distance2d: %.3f, distance3d: %.3f.",
                    result.getSampleCount(),
                    result.getOutputFile().getPath(),
                    result.getDurationSeconds(),
                    result.getTotalDistance2d(),
                    result.getTotalDistance3d()
            ));
        } catch (IOException exception) {
            throw new CommandException("Failed to save data capture: " + exception.getMessage());
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
                    "Data capture is stopped. Buffered samples: %d, distance2d: %.3f, distance3d: %.3f.",
                    status.getSampleCount(),
                    status.getTotalDistance2d(),
                    status.getTotalDistance3d()
            ));
            return;
        }

        send(sender, String.format(
                "Data capture is running for %s. Samples: %d, duration: %.3fs, distance2d: %.3f, distance3d: %.3f.",
                status.getPlayerName(),
                status.getSampleCount(),
                status.getDurationSeconds(),
                status.getTotalDistance2d(),
                status.getTotalDistance3d()
        ));
    }

    private void executeReset(ICommandSender sender) {
        ResetResult result = recorder.reset();
        send(sender, "Data capture reset. Removed " + result.getRemovedSampleCount() + " buffered samples.");
    }

    private void executeExit(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            send(sender, "/datacap exit <create|remove|list|info>");
            return;
        }

        if ("create".equalsIgnoreCase(args[1])) {
            executeExitCreate(sender, args);
        } else if ("remove".equalsIgnoreCase(args[1])) {
            executeExitRemove(sender, args);
        } else if ("list".equalsIgnoreCase(args[1])) {
            requireArgumentCount(sender, args, 2);
            executeExitList(sender);
        } else if ("info".equalsIgnoreCase(args[1])) {
            executeExitInfo(sender, args);
        } else {
            send(sender, "/datacap exit <create|remove|list|info>");
        }
    }

    private void executeExitCreate(ICommandSender sender, String[] args) throws CommandException {
        requireArgumentCount(sender, args, 3);
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        String id = args[2];
        validateRegionId(id);

        Selection selection = DataCollectionMod.SELECTIONS.getSelection(player.getUniqueID());
        if (selection == null || !selection.isComplete()) {
            throw new CommandException("Select pos1 with left click and pos2 with right click first.");
        }

        ExitRegion region = new ExitRegion(id, selection.getPos1(), selection.getPos2());
        if (!DataCollectionMod.EXIT_REGIONS.add(region)) {
            throw new CommandException("Exit region already exists: " + id);
        }

        send(sender, "Created exit region " + id + " from "
                + format(region.getMin()) + " to " + format(region.getMax()) + ".");
    }

    private void executeExitRemove(ICommandSender sender, String[] args) throws CommandException {
        requireArgumentCount(sender, args, 3);
        String id = args[2];
        if (DataCollectionMod.EXIT_REGIONS.remove(id)) {
            send(sender, "Removed exit region " + id + ".");
        } else {
            throw new CommandException("Unknown exit region: " + id);
        }
    }

    private void executeExitList(ICommandSender sender) {
        Collection<ExitRegion> regions = DataCollectionMod.EXIT_REGIONS.getAll();
        if (regions.isEmpty()) {
            send(sender, "No exit regions defined.");
            return;
        }

        StringBuilder builder = new StringBuilder("Exit regions:");
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
            throw new CommandException("Unknown exit region: " + id);
        }

        send(sender, "Exit region " + id + ": min " + format(region.getMin())
                + ", max " + format(region.getMax()) + ".");
    }

    private void requireArgumentCount(ICommandSender sender, String[] args, int expected) throws CommandException {
        if (args.length != expected) {
            throw new CommandException(getUsage(sender));
        }
    }

    private void validateRegionId(String id) throws CommandException {
        if (!id.matches(ID_PATTERN)) {
            throw new CommandException("Exit region id may only contain letters, numbers, _, -, :, and .");
        }
    }

    private String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private void send(ICommandSender sender, String message) {
        sender.sendMessage(new TextComponentString(message));
    }
}
