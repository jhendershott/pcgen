/**
 * pcgen.core.term.BaseEQTermEvaluator.java
 * Copyright (c) 2008 Andrew Wilson <nuance@users.sourceforge.net>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Created 03 October 2008 
 *
 * Current Ver: $Revision:$
 *
 */

package pcgen.core.term;

import pcgen.core.PlayerCharacter;
import pcgen.core.character.CharacterSpell;
import pcgen.core.spell.Spell;
import pcgen.util.Logging;

public abstract class BaseEQTermEvaluator
{
	protected String originalText;

	public String evaluate(PlayerCharacter pc) {
		return "0.0";
	}

	public String evaluate(PlayerCharacter pc,  final Spell aSpell) {
		return "0.0";
	}

	public Float resolve(PlayerCharacter pc)
	{
		return convertToFloat(originalText, evaluate(pc));
	}

	public Float resolve(PlayerCharacter pc, final CharacterSpell aSpell) {
		return convertToFloat(originalText, evaluate(pc, aSpell == null ? null : aSpell.getSpell()));
	}

	protected Float convertToFloat(String element, String foo)
	{
		Float d = null;
		try
		{
			d = new Float(foo);
		}
		catch (NumberFormatException nfe)
		{
			// What we got back was not a number
		}

		Float retVal = null;
		if (d != null && !d.isNaN())
		{
			retVal = d;
			if (Logging.isDebugMode())
			{
				Logging.debugPrint(new StringBuilder("Export variable for: '")
					.append(element).append("' = ").append(d).toString());
			}
		}

		return retVal;
	}

}

