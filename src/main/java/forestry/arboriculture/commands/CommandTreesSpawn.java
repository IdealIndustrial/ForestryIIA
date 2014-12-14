/*******************************************************************************
 * Copyright (c) 2011-2014 SirSengir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Various Contributors including, but not limited to:
 * SirSengir (original work), CovertJaguar, Player, Binnie, MysteriousAges
 ******************************************************************************/
package forestry.arboriculture.commands;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.gen.feature.WorldGenerator;

import forestry.api.arboriculture.IAlleleTreeSpecies;
import forestry.api.arboriculture.ITree;
import forestry.api.arboriculture.ITreeGenome;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAllele;
import forestry.core.commands.CommandHelpers;
import forestry.core.commands.SpeciesNotFoundException;
import forestry.core.commands.SubCommand;
import forestry.core.commands.TemplateNotFoundException;
import forestry.core.worldgen.WorldGenBase;
import forestry.plugins.PluginArboriculture;

import org.apache.commons.lang3.StringUtils;

public abstract class CommandTreesSpawn extends SubCommand {

	public CommandTreesSpawn(String name) {
		super(name);
		setPermLevel(PermLevel.ADMIN);
	}

	@Override
	public final void processSubCommand(ICommandSender sender, String[] arguments) {
		if (arguments.length < 1 || arguments.length > 2) {
			printHelp(sender);
			return;
		}

		EntityPlayer player;
		String treeName;
		try {
			player = CommandHelpers.getPlayer(sender, arguments[arguments.length - 1]);
			String[] argumentsWithoutPlayer = new String[arguments.length - 1];
			System.arraycopy(arguments, 0, argumentsWithoutPlayer, 0, arguments.length - 1);
			treeName = StringUtils.join(argumentsWithoutPlayer, " ");
		} catch (PlayerNotFoundException e) {
			player = CommandHelpers.getPlayer(sender, sender.getCommandSenderName());
			treeName = StringUtils.join(arguments, " ");
		}

		processSubCommand(sender, treeName, player);
	}

	protected abstract void processSubCommand(ICommandSender sender, String treeName, EntityPlayer player);

	protected final WorldGenerator getWorldGen(String treeName, EntityPlayer player, int x, int y, int z) {
		ITreeGenome treeGenome = getTreeGenome(treeName);
		if (treeGenome == null) {
			return null;
		}

		ITree tree = PluginArboriculture.treeInterface.getTree(player.worldObj, treeGenome);
		return tree.getTreeGenerator(player.worldObj, x, y, z, true);
	}

	protected final void generateTree(WorldGenerator gen, EntityPlayer player, int x, int y, int z) {
		if (gen instanceof WorldGenBase) {
			((WorldGenBase) gen).generate(player.worldObj, player.worldObj.rand, x, y, z, true);
		} else {
			gen.generate(player.worldObj, player.worldObj.rand, x, y, z);
		}
	}

	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] parameters) {
		if (parameters.length == 1) {
			List<String> tabCompletion = CommandHelpers.getListOfStringsMatchingLastWord(parameters, getSpecies());
			tabCompletion.add("help");
			return tabCompletion;
		} else if (parameters.length == 2) {
			return CommandHelpers.getListOfStringsMatchingLastWord(parameters, CommandHelpers.getPlayers());
		}
		return null;
	}

	private static String[] getSpecies() {
		List<String> species = new ArrayList<String>();

		for (IAllele allele : AlleleManager.alleleRegistry.getRegisteredAlleles().values())
			if (allele instanceof IAlleleTreeSpecies)
				species.add(allele.getName().replaceAll("\\s", ""));

		return species.toArray(new String[species.size()]);
	}

	private static ITreeGenome getTreeGenome(String speciesName) {
		IAlleleTreeSpecies species = null;

		for (String uid : AlleleManager.alleleRegistry.getRegisteredAlleles().keySet()) {

			if (!uid.equals(speciesName))
				continue;

			IAllele allele = AlleleManager.alleleRegistry.getAllele(uid);
			if (allele instanceof IAlleleTreeSpecies) {
				species = (IAlleleTreeSpecies) allele;
				break;
			}
		}

		if (species == null) {
			for (IAllele allele : AlleleManager.alleleRegistry.getRegisteredAlleles().values()) {
				if (allele instanceof IAlleleTreeSpecies && allele.getName().replaceAll("\\s", "").equals(speciesName)) {
					species = (IAlleleTreeSpecies) allele;
					break;
				}
			}
		}

		if (species == null)
			throw new SpeciesNotFoundException(speciesName);

		IAllele[] template = PluginArboriculture.treeInterface.getTemplate(species.getUID());

		if (template == null)
			throw new TemplateNotFoundException(species);

		return PluginArboriculture.treeInterface.templateAsGenome(template);
	}

}
