package thunder.hack.utility.interfaces;

import java.util.List;
import net.minecraft.util.math.BlockPos;
import thunder.hack.features.modules.render.Trails;

public interface IEntity {
   List<Trails.Trail> getTrails();

   BlockPos thunderHack_Recode$getVelocityBP();
}
