package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Direct-chat session (2026-08-26, right after v0.21.0): the mod's first
 * custom {@link SoundEvent}s. Until now every sound played by this mod
 * (block break/place via {@code SoundType}, the portal's ignite sound via
 * {@code SoundEvents.PORTAL_TRIGGER}, the teleport whoosh via {@code
 * SoundEvents.ENDERMAN_TELEPORT}, etc.) reused a vanilla sound wholesale.
 * The repo owner asked specifically for original ignite/collapse sounds
 * for {@link com.claudemod.block.PrismiumPortalBlock}, so this registry
 * exists to hold those two (and any future custom sounds).
 *
 * <p><b>Audio is synthesized, not recorded/sampled from anywhere</b> - see
 * {@code scripts/sounds/gen_portal_sounds.py} (numpy, stdlib
 * {@code wave} module, then {@code ffmpeg -c:a libvorbis} to produce the
 * {@code .ogg} Minecraft requires). {@code ignite.ogg} is a rising pitch
 * sweep + a swelling high-frequency shimmer + a short crystalline chime
 * accent near the end (echoing this mod's established "Prismium is a
 * chiming crystal" sound language - see the {@code SoundType.AMETHYST}
 * usage on several other Prismium blocks); {@code fizzle.ogg} is the
 * inverse: a falling pitch sweep + glassy crackle bursts + a short
 * descending "un-ringing" chime, for when a portal's frame is broken and
 * its interior collapses (see {@link
 * com.claudemod.event.PrismiumPortalFrameBreakHandler}).
 *
 * <p><b>Self-review, in place of listening (this sandbox cannot play
 * audio)</b>: the generation script prints peak amplitude, RMS, duration
 * and a NaN check for each waveform before encoding, and both were
 * confirmed to have no clipping (peak &lt; 1.0), no silence/dead air
 * (RMS in a reasonable 0.2-0.3 range), correct duration, and no invalid
 * samples. {@code ffprobe} was used to confirm both encoded {@code .ogg}
 * files are valid Vorbis streams (44.1kHz mono) that decode cleanly.
 * <b>What this self-review cannot cover, unlike this mod's usual texture
 * self-review process</b>: whether the sounds actually sound good/fitting
 * once played - this sandbox has no audio output device. The repo owner
 * did listen (direct-chat session, same day) and reported the first
 * version felt "flat"/monotonous, which lines up with a known limitation
 * of simple additive/parametric synthesis. Two follow-up changes were
 * made in response: (1) the synthesis itself was reworked to use
 * detuned-unison oscillators, slow organic pitch jitter, mild waveshaping
 * and a small algorithmic reverb instead of clean single sine waves (see
 * {@code detuned_sweep}/{@code make_ir}/{@code apply_reverb} in the
 * generation script); (2) per the repo owner's own suggestion, the
 * synthesized chime layer was dropped entirely in favor of layering real
 * vanilla {@code SoundEvent}s ({@code SoundEvents.AMETHYST_BLOCK_CHIME}
 * on ignite, {@code SoundEvents.AMETHYST_BLOCK_RESONATE} at a lowered
 * pitch on fizzle - see {@link com.claudemod.event.PrismiumPortalIgniteHandler}
 * / {@link com.claudemod.event.PrismiumPortalFrameBreakHandler}) for
 * genuine musical/tonal content a synthesizer can't easily fake, keeping
 * the Python synthesis for the atmospheric whoosh/shimmer/crackle texture
 * vanilla doesn't have an equivalent for. This mix-vanilla-and-synthesis
 * approach is now a standing rule recorded in PROGRESS.md section 0.
 * Still genuinely unverified whether this second pass reads better -
 * that's for the next listen.
 */
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ClaudeMod.MOD_ID);

    public static final RegistryObject<SoundEvent> PRISMIUM_PORTAL_IGNITE = SOUND_EVENTS.register(
            "block.prismium_portal.ignite",
            () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(ClaudeMod.MOD_ID, "block.prismium_portal.ignite")));

    public static final RegistryObject<SoundEvent> PRISMIUM_PORTAL_FIZZLE = SOUND_EVENTS.register(
            "block.prismium_portal.fizzle",
            () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(ClaudeMod.MOD_ID, "block.prismium_portal.fizzle")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
