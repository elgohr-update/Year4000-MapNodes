package net.year4000.mapnodes.games;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.ToString;
import net.year4000.mapnodes.api.exceptions.InvalidJsonException;
import net.year4000.mapnodes.api.game.modes.GameModeConfig;
import net.year4000.mapnodes.api.game.modes.GameModeConfigName;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

@ToString
@Getter
@GameModeConfigName("builders")
public class BuildersConfig implements GameModeConfig {
    private static final int HEIGHT = 256;
    private int height = Byte.MAX_VALUE;
    private List<Plot> plots = Lists.newArrayList();

    @Override
    public void validate() throws InvalidJsonException {
        checkArgument(height > 0, "height is not > 0");
    }

    /**
     * The object that makes up a plot point with min and max
     */
    public class Plot {
        private String min;
        private String max;

        /** Translate a string seperated by commands into int array */
        private int[] transcribe(String input) {
            checkNotNull(input, "input");
            String[] parts = input.split(",");
            checkArgument(parts.length == 2);

            int x = Integer.parseInt(parts[0].replaceAll(" ", ""));
            int z = Integer.parseInt(parts[1].replaceAll(" ", ""));

            return new int[] {x, z};
        }

        /** Grab the BlockVector of min plot */
        public BlockVector getMin() {
            int[] xz = transcribe(min);

            return new BlockVector(xz[0], 0, xz[1]);
        }

        /** Grab the BlockVector of max plot */
        public BlockVector getMax() {
            int[] xz = transcribe(max);

            return new BlockVector(xz[0], HEIGHT, xz[1]);
        }

        /** Grab the BlockVector of min plot */
        public BlockVector getInnerMin() {
            int[] xz = transcribe(min);

            int offsetX = xz[0] < getMax().getBlockX() ? 1 : -1;
            int offsetZ = xz[1] < getMax().getBlockZ() ? 1 : -1;

            return new BlockVector(xz[0] + offsetX, 0, xz[1] + offsetZ);
        }

        /** Grab the BlockVector of max plot */
        public BlockVector getInnerMax() {
            int[] xz = transcribe(max);

            int offsetX = xz[0] < getMin().getBlockX() ? 1 : -1;
            int offsetZ = xz[1] < getMin().getBlockZ() ? 1 : -1;

            return new BlockVector(xz[0] + offsetX, HEIGHT, xz[1] + offsetZ);
        }

        /** Grab the BlockVector of min plot */
        public BlockVector getOuterMin() {
            int[] xz = transcribe(min);

            int offsetX = xz[0] > getMax().getBlockX() ? 1 : -1;
            int offsetZ = xz[1] > getMax().getBlockZ() ? 1 : -1;

            return new BlockVector(xz[0] + offsetX, 0, xz[1] + offsetZ);
        }

        /** Grab the BlockVector of max plot */
        public BlockVector getOuterMax() {
            int[] xz = transcribe(max);

            int offsetX = xz[0] > getMin().getBlockX() ? 1 : -1;
            int offsetZ = xz[1] > getMin().getBlockZ() ? 1 : -1;

            return new BlockVector(xz[0] + offsetX, HEIGHT, xz[1] + offsetZ);
        }

        /** Is the specific vector inside region */
        public boolean isInPlot(Vector vector) {
            return isInPlot(vector, 0, HEIGHT);
        }

        /** Is the specific vector inside region */
        public boolean isInPlot(Vector vector, int minY, int maxY) {
            Vector min = getMin().setY(minY);
            Vector max = getOuterMax().setY(maxY);

            return isInPlot(min, max, vector);
        }

        /** Is the specific vector inside region */
        public boolean isInInnerPlot(Vector vector) {
            return isInInnerPlot(vector, 0, HEIGHT);
        }

        /** Is the specific vector inside region */
        public boolean isInInnerPlot(Vector vector, int minY, int maxY) {
            Vector min = getInnerMin().setY(minY);
            Vector max = getInnerMax().setY(maxY);

            return isInPlot(min, max, vector);
        }

        /** Is the specific vector inside region */
        public boolean isInPlot(Vector min, Vector max, Vector vector) {
            int minX = min.getBlockX() > max.getBlockX() ? max.getBlockX() : min.getBlockX();
            int minZ = min.getBlockZ() > max.getBlockZ() ? max.getBlockZ() : min.getBlockZ();
            int maxX = min.getBlockX() < max.getBlockX() ? max.getBlockX() : min.getBlockX();
            int maxZ = min.getBlockZ() < max.getBlockZ() ? max.getBlockZ() : min.getBlockZ();

            Vector trueMin = new Vector(minX, min.getY(), minZ);
            Vector trueMax = new Vector(maxX, max.getY(), maxZ);

            return vector.isInAABB(trueMin, trueMax);
        }
    }
}
