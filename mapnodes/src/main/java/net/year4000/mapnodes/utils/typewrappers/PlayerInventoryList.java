/*
 * Copyright 2015 Year4000. All Rights Reserved.
 */

package net.year4000.mapnodes.utils.typewrappers;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.year4000.mapnodes.game.kits.SlotItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PlayerInventoryList<T> extends ArrayList<T> implements List<T> {
    private List<SlotItem> rawItems = new ArrayList<>();

    public List<ItemStack> getNonAirItems() {
        List<ItemStack> list = new ArrayList<>();

        rawItems.forEach(item -> {
            ItemStack itemStack = item.create();

            if (itemStack.getType() != Material.AIR) {
                list.add(itemStack);
            }
        });

        return list;
    }
}