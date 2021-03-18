package net.barribob.boss.mob.mobs.gauntlet

import net.barribob.boss.mob.ai.action.IActionWithCooldown
import net.barribob.boss.utils.VanillaCopies
import net.barribob.maelstrom.general.event.EventScheduler
import net.barribob.maelstrom.general.event.TimedEvent
import net.barribob.maelstrom.static_utilities.MathUtils
import net.barribob.maelstrom.static_utilities.addVelocity
import net.barribob.maelstrom.static_utilities.eyePos
import net.barribob.maelstrom.static_utilities.planeProject
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.util.math.Vec3d

class SwirlPunchAction(val entity: GauntletEntity, val eventScheduler: EventScheduler) : IActionWithCooldown {
    private var previousSpeed = 0.0

    override fun perform(): Int {
        val target = entity.target ?: return 40
        val targetPos = target.boundingBox.center
        val accelerateStartTime = 30
        val unclenchTime = 60
        val closeFistAnimationTime = 7

        entity.addVelocity(0.0, 0.7, 0.0)
        entity.dataTracker.set(isEnergized, true)
        eventScheduler.addEvent(TimedEvent(entity.hitboxHelper::setClosedFistHitbox, closeFistAnimationTime))
        eventScheduler.addEvent(TimedEvent({accelerateTowardsTarget(targetPos)}, accelerateStartTime, 9))
        eventScheduler.addEvent(TimedEvent(::whilePunchActive, accelerateStartTime, unclenchTime - accelerateStartTime))
        eventScheduler.addEvent(TimedEvent({
            entity.hitboxHelper.setOpenHandHitbox()
            entity.dataTracker.set(isEnergized, false)
        }, unclenchTime))

        return 80
    }

    private fun whilePunchActive() {
        testBlockPhysicalImpact()
        testEntityImpact()
        val speed = entity.velocity.length()
//        if(speed > 0.25) entity.destroyBlocks(Box(entity.pos, entity.pos).expand(1.0).offset(0.0, -0.5, 0.0))
        previousSpeed = speed
    }

    private fun accelerateTowardsTarget(target: Vec3d){
        val dir: Vec3d = MathUtils.unNormedDirection(entity.eyePos(), target).normalize()
        val velocityCorrection: Vec3d = entity.velocity.planeProject(dir)
        entity.addVelocity(dir.subtract(velocityCorrection).multiply(0.40))
    }

    private fun testBlockPhysicalImpact() {
        if((entity.horizontalCollision || entity.verticalCollision) && previousSpeed > 0.55f) {
            val pos: Vec3d = entity.pos
            val flag = VanillaCopies.getEntityDestructionType(entity.world)
            if(entity.dataTracker.get(isEnergized)) {
                entity.world.createExplosion(entity, pos.x, pos.y, pos.z, 4.5f, true, flag)
                entity.dataTracker.set(isEnergized, false)
            } else {
                entity.world.createExplosion(entity, pos.x, pos.y, pos.z, (previousSpeed * 1.5).toFloat(), flag)
            }
        }
    }

    private fun testEntityImpact(){
        val collidedEntities = entity.world.getEntitiesByClass(LivingEntity::class.java, entity.boundingBox) { it != entity }
        for(target in collidedEntities){
            entity.tryAttack(target) // Todo: customize attack interactions
            target.addVelocity(entity.velocity.multiply(0.5))
        }
    }

    companion object {
        val isEnergized: TrackedData<Boolean> =
            DataTracker.registerData(GauntletEntity::class.java, TrackedDataHandlerRegistry.BOOLEAN)
    }
}