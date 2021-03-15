package net.barribob.boss.mob.mobs.gauntlet

import net.barribob.boss.mob.ai.action.IActionWithCooldown
import net.barribob.boss.utils.VanillaCopies
import net.barribob.boss.utils.VanillaCopies.destroyBlocks
import net.barribob.maelstrom.general.event.EventScheduler
import net.barribob.maelstrom.general.event.TimedEvent
import net.barribob.maelstrom.static_utilities.MathUtils
import net.barribob.maelstrom.static_utilities.addVelocity
import net.barribob.maelstrom.static_utilities.eyePos
import net.barribob.maelstrom.static_utilities.planeProject
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

class PunchAction(val entity: GauntletEntity, val eventScheduler: EventScheduler) : IActionWithCooldown {
    private var previousSpeed = 0.0

    override fun perform(): Int {
        val target = entity.target ?: return 40
        val targetPos = target.eyePos()
        val accelerateStartTime = 16
        val unclenchTime = 60

        entity.addVelocity(0.0, 0.5, 0.0)
        eventScheduler.addEvent(TimedEvent({accelerateTowardsTarget(targetPos)}, accelerateStartTime, 9))
        eventScheduler.addEvent(TimedEvent(::whilePunchActive, accelerateStartTime, unclenchTime - accelerateStartTime))

        val closeFistAnimationTime = 7
        eventScheduler.addEvent(TimedEvent(entity.hitboxHelper::setClosedFistHitbox, closeFistAnimationTime))

        eventScheduler.addEvent(TimedEvent({
            entity.hitboxHelper.setOpenHandHitbox()
            entity.world.sendEntityStatus(
                entity,
                GauntletAttacks.stopPunchAnimation
            )
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
        entity.addVelocity(dir.subtract(velocityCorrection).multiply(0.32))
    }

    private fun testBlockPhysicalImpact() {
        if((entity.horizontalCollision || entity.verticalCollision) && previousSpeed > 0.55f) {
            val pos: Vec3d = entity.pos
            val flag = VanillaCopies.getEntityDestructionType(entity.world)
            entity.world.createExplosion(entity, pos.x, pos.y, pos.z, (previousSpeed * 1.5).toFloat(), flag)
        }
    }

    private fun testEntityImpact(){
        val collidedEntities = entity.world.getEntitiesByClass(LivingEntity::class.java, entity.boundingBox) { it != entity }
        for(target in collidedEntities){
            entity.tryAttack(target) // Todo: customize attack interactions
            target.addVelocity(entity.velocity.multiply(0.5))
        }
    }
}