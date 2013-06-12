package org.powerbot.script.methods;

import org.powerbot.script.methods.ClientFactory;
import org.powerbot.script.methods.ClientLink;
import org.powerbot.script.util.*;
import org.powerbot.script.wrappers.Component;
import org.powerbot.script.wrappers.GameObject;
import org.powerbot.script.wrappers.Item;
import org.powerbot.script.wrappers.Widget;

import java.util.Arrays;

public class DepositBox extends ClientLink {
	public static final int[] DEPOSIT_BOX_IDS = new int[]{
			2045, 2133, 6396, 6402, 6404, 6417, 6418, 6453, 6457, 6478, 6836, 9398, 15985, 20228, 24995, 25937, 26969,
			32924, 32930, 32931, 34755, 36788, 39830, 45079, 66668, 70512, 73268
	};
	public static final int WIDGET = 11;
	public static final int COMPONENT_BUTTON_CLOSE = 15;
	public static final int COMPONENT_CONTAINER_ITEMS = 17;
	public static final int COMPONENT_BUTTON_DEPOSIT_INVENTORY = 19;
	public static final int COMPONENT_BUTTON_DEPOSIT_EQUIPMENT = 23;
	public static final int COMPONENT_BUTTON_DEPOSIT_FAMILIAR = 25;
	public static final int COMPONENT_BUTTON_DEPOSIT_POUCH = 21;

	public DepositBox(ClientFactory factory) {
		super(factory);
	}

	public boolean isOpen() {
		final Widget widget = ctx.widgets.get(WIDGET);
		return widget != null && widget.isValid();
	}

	public boolean open() {
		if (isOpen()) return true;
		GameObject object = Filters.nearest(Filters.id(ctx.objects.getLoaded(), DEPOSIT_BOX_IDS), ctx.players.getLocal());
		if (object.interact("Deposit")) {
			final Widget bankPin = ctx.widgets.get(13);
			for (int i = 0; i < 20 && !isOpen() && !bankPin.isValid(); i++) Delay.sleep(200, 300);
		}
		return isOpen();
	}

	public boolean close(final boolean wait) {
		if (!isOpen()) return true;
		final Component c = ctx.widgets.get(WIDGET, COMPONENT_BUTTON_CLOSE);
		if (c == null) return false;
		if (c.isValid() && c.interact("Close")) {
			if (!wait) return true;
			final Timer t = new Timer(Random.nextInt(1000, 2000));
			while (t.isRunning() && isOpen()) Delay.sleep(100);
			return !isOpen();
		}
		return false;
	}

	public boolean close() {
		return close(true);
	}

	public Item[] getItems() {
		final Component c = ctx.widgets.get(WIDGET, COMPONENT_CONTAINER_ITEMS);
		if (c == null || !c.isValid()) return new Item[0];
		final Component[] components = c.getChildren();
		Item[] items = new Item[components.length];
		int d = 0;
		for (final Component i : components) if (i.getItemId() != -1) items[d++] = new Item(ctx, i);
		return Arrays.copyOf(items, d);
	}

	public Item[] getItems(final int... ids) {
		Arrays.sort(ids);
		return getItems(new Filter<Item>() {
			@Override
			public boolean accept(final Item item) {
				return Arrays.binarySearch(ids, item.getId()) >= 0;
			}
		});
	}

	public Item getItem(final int... ids) {
		final Item[] items = getItems(ids);
		return items.length > 0 ? items[0] : null;
	}

	public Item getItem(final Filter<Item> filter) {
		final Item[] items = getItems(filter);
		return items.length > 0 ? items[0] : null;
	}

	public Item[] getItems(final Filter<Item> filter) {
		final Item[] items = getItems();
		final Item[] arr = new Item[items.length];
		int d = 0;
		for (final Item item : items) if (filter.accept(item)) arr[d++] = item;
		return Arrays.copyOf(arr, d);
	}

	public Item getItemAt(final int index) {
		final Component c = ctx.widgets.get(WIDGET, COMPONENT_CONTAINER_ITEMS);
		if (c == null || !c.isValid()) return null;
		final Component i = c.getChild(index);
		if (i != null && i.getItemId() != -1) return new Item(ctx, i);
		return null;
	}

	public int indexOf(final int id) {
		final Component items = ctx.widgets.get(WIDGET, COMPONENT_CONTAINER_ITEMS);
		if (items == null || !items.isValid()) return -1;
		final Component[] comps = items.getChildren();
		for (int i = 0; i < comps.length; i++) if (comps[i].getItemId() == id) return i;
		return -1;
	}

	public boolean contains(final int id) {
		return indexOf(id) != -1;
	}

	public boolean containsAll(final int... ids) {
		for (final int id : ids) if (indexOf(id) == -1) return false;
		return true;
	}

	public boolean containsOneOf(final int... ids) {
		for (final int id : ids) if (indexOf(id) != -1) return true;
		return false;
	}

	public int getCount() {
		return getCount(false);
	}

	public int getCount(final boolean stacks) {
		int count = 0;
		final Item[] items = getItems();
		for (final Item item : items) {
			if (stacks) count += item.getStackSize();
			else ++count;
		}
		return count;
	}

	public int getCount(final int... ids) {
		return getCount(false, ids);
	}

	public int getCount(final boolean stacks, final int... ids) {
		int count = 0;
		final Item[] items = getItems();
		for (final Item item : items) {
			for (final int id : ids) {
				if (item.getId() == id) {
					if (stacks) count += item.getStackSize();
					else ++count;
					break;
				}
			}
		}
		return count;
	}

	public boolean deposit(final int id, final int amount) {
		if (!isOpen() || amount < 0) return false;
		final Item item = ctx.inventory.getItem(id);
		if (item == null) return false;
		String action = "Deposit-" + amount;
		final int c = ctx.inventory.getCount(true, id);
		if (c == 1) action = "Depoist";
		else if (c <= amount || amount == 0) {
			action = "Deposit-All";
		}

		final Component comp = item.getComponent();
		final int inv = ctx.inventory.getCount(true);
		if (containsAction(comp, action)) {
			if (!comp.interact(action)) return false;
		} else {
			if (!comp.interact("Withdraw-X")) return false;
			for (int i = 0; i < 20 && !isInputWidgetOpen(); i++) Delay.sleep(100, 200);
			if (!isInputWidgetOpen()) return false;
			Delay.sleep(200, 800);
			ctx.keyboard.sendln(amount + "");
		}
		for (int i = 0; i < 25 && ctx.inventory.getCount(true) == inv; i++) Delay.sleep(100, 200);
		return ctx.inventory.getCount(true) != inv;
	}

	public boolean depositctInventory() {
		final Component c = ctx.widgets.get(WIDGET, COMPONENT_BUTTON_DEPOSIT_INVENTORY);
		if (c == null || !c.isValid()) return false;
		if (ctx.inventory.isEmpty()) return true;
		final int inv = ctx.inventory.getCount(true);
		if (c.click()) for (int i = 0; i < 25 && ctx.inventory.getCount(true) == inv; i++) Delay.sleep(100, 200);
		return ctx.inventory.getCount(true) != inv;
	}

	public boolean depositEquipment() {
		final Component c = ctx.widgets.get(WIDGET, COMPONENT_BUTTON_DEPOSIT_EQUIPMENT);
		return c != null && c.isValid() && c.click();
	}

	public boolean depositFamiliar() {
		final Component c = ctx.widgets.get(WIDGET, COMPONENT_BUTTON_DEPOSIT_FAMILIAR);
		return c != null && c.isValid() && c.click();
	}

	public boolean depositPouch() {
		final Component c = ctx.widgets.get(WIDGET, COMPONENT_BUTTON_DEPOSIT_POUCH);
		return c != null && c.isValid() && c.click();
	}

	private boolean containsAction(final Component c, final String action) {
		final String[] actions = c.getActions();
		if (action == null) return false;
		for (final String a : actions) if (a != null && a.matches("^" + action + "(<.*>)?$")) return true;
		return false;
	}

	private boolean isInputWidgetOpen() {
		final Component child = ctx.widgets.get(752, 3);
		return child != null && child.isValid() && child.isOnScreen();
	}
}
