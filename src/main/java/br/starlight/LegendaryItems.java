package br.starlight;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public final class LegendaryItems implements Listener {
    private final StarLight plugin;
    private final NamespacedKey idKey, twinKey, compassKey;
    private final Map<String,Long> cooldown = new HashMap<>();
    private final Map<UUID,UUID> compassTargets = new HashMap<>();
    private final Map<UUID,UUID> twinPartners = new HashMap<>();
    private final Map<UUID,UUID> disguised = new HashMap<>();
    private final Set<UUID> invisPlayers = new HashSet<>();

    private static final String MENU="§6§l⭐ STARLIGHT ⭐";
    private static final String TARGET_MENU="§e§l🧭 ALVOS";

    public LegendaryItems(StarLight p) {
        plugin=p;
        idKey=new NamespacedKey(p,"legendary_id");
        twinKey=new NamespacedKey(p,"twin_partner");
        compassKey=new NamespacedKey(p,"compass_target");
        startTasks();
    }

    private ItemStack make(Material mat,String name,String id) {
        ItemStack i=new ItemStack(mat);
        ItemMeta m=i.getItemMeta();
        m.displayName(Component.text(name,NamedTextColor.YELLOW,TextDecoration.BOLD,TextDecoration.ITALIC));
        m.setUnbreakable(true);
        m.getPersistentDataContainer().set(idKey,PersistentDataType.STRING,id);
        i.setItemMeta(m);
        return i;
    }
    private String id(ItemStack i) {
        if(i==null||!i.hasItemMeta()) return "";
        return i.getItemMeta().getPersistentDataContainer().getOrDefault(idKey,PersistentDataType.STRING,"");
    }
    private boolean has(Player p,String id) {
        for(ItemStack i:p.getInventory().getContents()) if(id(i).equals(id)) return true;
        return false;
    }
    private boolean hasEither(Player p,String a,String b) { return has(p,a)||has(p,b); }

    public void openMenu(Player p) {
        Inventory inv=Bukkit.createInventory(null,54,Component.text(MENU));
        String[] ids={"relogio","lampiao","bussola","gemeos_a","gemeos_b","escudo","bota","batata","dragao","pocao","tridente","picareta","cacador","pena"};
        for(int s=0;s<ids.length;s++) inv.setItem(s,create(ids[s]));
        p.openInventory(inv);
    }
    private ItemStack create(String id) {
        Material m=switch(id) {
            case "lampiao"->Material.SOUL_LANTERN; case "bussola"->Material.COMPASS;
            case "gemeos_a","gemeos_b"->Material.TURTLE_HELMET; case "escudo"->Material.SHIELD;
            case "bota"->Material.DIAMOND_BOOTS; case "batata"->Material.POISONOUS_POTATO;
            case "dragao"->Material.DRAGON_HEAD; case "pocao"->Material.POTION;
            case "tridente"->Material.TRIDENT; case "picareta"->Material.IRON_PICKAXE;
            case "cacador"->Material.GOLDEN_SWORD; case "pena"->Material.FEATHER;
            default->Material.CLOCK;
        };
        String n=switch(id) {
            case "relogio"->"Relógio Lendário"; case "lampiao"->"Lampião Lendário";
            case "bussola"->"Bússola Lendária"; case "gemeos_a"->"Capacete dos Gêmeos §f(A)";
            case "gemeos_b"->"Capacete dos Gêmeos §f(B)"; case "escudo"->"Escudo Lendário";
            case "bota"->"Bota Lendária"; case "batata"->"Batata do Bobó";
            case "dragao"->"Cabeça do Dragão"; case "pocao"->"Poção Lendária";
            case "tridente"->"Tridente Lendário"; case "picareta"->"Picareta Lendária";
            case "cacador"->"Espada do Caçador"; default->"Pena Lendária";
        };
        ItemStack i=make(m,n,id);
        ItemMeta meta=i.getItemMeta();
        if(Set.of("escudo","bota","tridente","picareta","cacador").contains(id)) meta.addEnchant(Enchantment.UNBREAKING,3,true);
        if(id.equals("escudo")) meta.addEnchant(Enchantment.THORNS,3,true);
        if(id.equals("cacador")) { meta.addEnchant(Enchantment.KNOCKBACK,10,true); meta.addEnchant(Enchantment.FIRE_ASPECT,2,true); }
        if(id.equals("picareta")) { meta.addEnchant(Enchantment.FORTUNE,3,true); meta.addEnchant(Enchantment.EFFICIENCY,10,true); }
        if(id.equals("tridente")) { meta.addEnchant(Enchantment.IMPALING,5,true); meta.addEnchant(Enchantment.LOYALTY,3,true); }
        i.setItemMeta(meta); return i;
    }

    @EventHandler public void menuClick(InventoryClickEvent e) {
        if(!e.getView().title().equals(Component.text(MENU))) return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p)||e.getCurrentItem()==null)return;
        String id=id(e.getCurrentItem()); if(id.isEmpty())return;
        p.getInventory().addItem(create(id)); p.sendMessage(Component.text("⭐ Item Lendário recebido!",NamedTextColor.YELLOW));
    }

    @EventHandler public void interact(PlayerInteractEvent e) {
        Player p=e.getPlayer(); ItemStack i=e.getItem(); String id=id(i);
        if(id.isEmpty())return;
        Action a=e.getAction();
        if(id.equals("relogio") && a.isRightClick()) { e.setCancelled(true); openTransformMenu(p); return; }
        if(id.equals("lampiao")) return;
        if(id.equals("bussola") && a.isRightClick()) { e.setCancelled(true); openTargetMenu(p); return; }
        if(id.equals("escudo") && a.isRightClick() && p.isSneaking()) { e.setCancelled(true); if(ready(p,"shield",10000)) sonic(p); return; }
        if(id.equals("dragao") && a.isLeftClick()) { e.setCancelled(true); if(ready(p,"dragon",10000)) dragonBreath(p); return; }
        if(id.equals("pocao") && a.isRightClick()) { e.setCancelled(true); if(ready(p,"blind",8000)) blind(p); return; }
        if(id.equals("cacador") && a.isRightClick()) {
            e.setCancelled(true); if(!ready(p,"raid",1500))return;
            p.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK,200,0,true,false,false));
        }
    }

    private void openTargetMenu(Player p) {
        Inventory inv=Bukkit.createInventory(null,54,Component.text(TARGET_MENU));
        for(Player t:Bukkit.getOnlinePlayers()) {
            ItemStack h=new ItemStack(Material.PLAYER_HEAD);
            SkullMeta m=(SkullMeta)h.getItemMeta(); m.setOwningPlayer(t);
            m.displayName(Component.text(t.getName(),NamedTextColor.YELLOW,TextDecoration.BOLD));
            m.getPersistentDataContainer().set(compassKey,PersistentDataType.STRING,t.getUniqueId().toString());
            h.setItemMeta(m); inv.addItem(h);
        }
        p.openInventory(inv);
    }
    @EventHandler public void targetClick(InventoryClickEvent e) {
        if(!e.getView().title().equals(Component.text(TARGET_MENU)))return;
        e.setCancelled(true); if(!(e.getWhoClicked() instanceof Player p)||e.getCurrentItem()==null)return;
        String raw=e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(compassKey,PersistentDataType.STRING);
        if(raw==null)return;
        UUID u=UUID.fromString(raw); if(Bukkit.getPlayer(u)==null)return;
        compassTargets.put(p.getUniqueId(),u); p.closeInventory();
        p.sendMessage(Component.text("🧭 Rastreando "+Bukkit.getPlayer(u).getName()+".",NamedTextColor.YELLOW));
    }

    private void openTransformMenu(Player p) {
        Inventory inv=Bukkit.createInventory(null,54,Component.text("§6§l⏱ TRANSFORMAÇÃO"));
        for(Player t:Bukkit.getOnlinePlayers()) {
            if(t.equals(p))continue;
            ItemStack h=new ItemStack(Material.PLAYER_HEAD);
            SkullMeta m=(SkullMeta)h.getItemMeta();m.setOwningPlayer(t);
            m.displayName(Component.text(t.getName(),NamedTextColor.YELLOW,TextDecoration.BOLD));
            m.getPersistentDataContainer().set(compassKey,PersistentDataType.STRING,t.getUniqueId().toString());
            h.setItemMeta(m);inv.addItem(h);
        }
        p.openInventory(inv);
    }
    @EventHandler public void transformClick(InventoryClickEvent e) {
        if(!e.getView().title().equals(Component.text("§6§l⏱ TRANSFORMAÇÃO")))return;
        e.setCancelled(true); if(!(e.getWhoClicked() instanceof Player p)||e.getCurrentItem()==null)return;
        String raw=e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(compassKey,PersistentDataType.STRING);
        if(raw==null)return; Player t=Bukkit.getPlayer(UUID.fromString(raw)); if(t==null)return;
        disguised.put(p.getUniqueId(),t.getUniqueId());
        p.playerListName(Component.text(t.getName()));
        p.displayName(Component.text(t.getName()));
        p.setPlayerProfile(t.getPlayerProfile());
        p.closeInventory();
        p.sendMessage(Component.text("⏱ Você assumiu a aparência de "+t.getName()+".",NamedTextColor.YELLOW));
    }

    private void sonic(Player p) {
        Location start=p.getEyeLocation();Vector d=start.getDirection().normalize();
        for(int n=1;n<=10;n++) {
            Location at=start.clone().add(d.clone().multiply(n));
            p.getWorld().spawnParticle(Particle.SONIC_BOOM,at,1);
            for(Entity x:p.getWorld().getNearbyEntities(at,.75,.75,.75))
                if(x instanceof LivingEntity l && x!=p) l.damage(10.0,p);
        }
        p.getWorld().playSound(p.getLocation(),Sound.ENTITY_WARDEN_SONIC_BOOM,1,1);
    }
    private void dragonBreath(Player p) {
        Location l=p.getEyeLocation();Vector d=l.getDirection().normalize();
        for(int n=1;n<=8;n++) {
            Location at=l.clone().add(d.clone().multiply(n));
            p.getWorld().spawnParticle(Particle.DRAGON_BREATH,at,20,.3,.3,.3,.02);
            for(Entity x:p.getWorld().getNearbyEntities(at,1,1,1))
                if(x instanceof LivingEntity le && x!=p) le.damage(2,p);
        }
    }
    private void blind(Player p) {
        for(Player t:Bukkit.getOnlinePlayers()) if(t!=p && t.getLocation().distanceSquared(p.getLocation())<100)
            t.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,100,0,true,false,false));
    }
    private boolean ready(Player p,String key,long ms) {
        String k=p.getUniqueId()+":"+key; long now=System.currentTimeMillis();
        if(now-cooldown.getOrDefault(k,0L)<ms)return false; cooldown.put(k,now); return true;
    }

    private void startTasks() {
        new BukkitRunnable(){public void run(){
            for(var e:compassTargets.entrySet()) {
                Player p=Bukkit.getPlayer(e.getKey()),t=Bukkit.getPlayer(e.getValue());
                if(p!=null&&t!=null)p.setCompassTarget(t.getLocation());
            }
            for(Player p:Bukkit.getOnlinePlayers()) updatePassive(p);
        }}.runTaskTimer(plugin,1L,plugin.getConfig().getLong("compass-update-ticks",2));

        new BukkitRunnable(){public void run(){ enforceTwinDeath(); }}.runTaskTimer(plugin,20L,20L);
    }
    private void updatePassive(Player p) {
        if(has(p,"lampiao")) {
            boolean inMain=id(p.getInventory().getItemInMainHand()).equals("lampiao");
            boolean inOff=id(p.getInventory().getItemInOffHand()).equals("lampiao");
            boolean on=inMain||inOff;
            if(on) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,40,0,true,false,false));
                invisPlayers.add(p.getUniqueId());
            } else if(invisPlayers.remove(p.getUniqueId())) p.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
        if(has(p,"bota") && (id(p.getInventory().getBoots()).equals("bota")))
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,40,2,true,false,false));
        if(has(p,"batata")) p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,40,4,true,false,false));
        if(has(p,"pena") && (id(p.getInventory().getItemInMainHand()).equals("pena")||id(p.getInventory().getItemInOffHand()).equals("pena")))
            p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION,20,0,true,false,false));
        if(has(p,"gemeos_a")||has(p,"gemeos_b")) {
            p.addPotionEffect(new PotionEffect(PotionEffectType
