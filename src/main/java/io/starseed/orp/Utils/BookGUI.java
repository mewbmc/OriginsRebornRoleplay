package io.starseed.orp.Utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.Material;

public class BookGUI {
    public static void openBook(Player player, String title, String defaultText, BookCompleteHandler handler) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setTitle(title);
            meta.setAuthor(player.getName());
            meta.addPage(defaultText);
            book.setItemMeta(meta);
        }
        player.openBook(book);
        // Add event listener to handle completion
    }

    public interface BookCompleteHandler {
        void onComplete(Player player, String text);
    }
}