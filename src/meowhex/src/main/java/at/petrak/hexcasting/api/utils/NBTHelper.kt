@file:JvmName("NBTHelper")

package at.petrak.hexcasting.api.utils

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.*
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.ItemStack
import java.util.*
import java.util.function.Consumer

private inline fun <T : Any, K, E> T?.getIf(key: K, predicate: T?.(K) -> Boolean, get: T.(K) -> E): E? =
    getIf(key, predicate, get, null)

private inline fun <T : Any, K, E> T?.getIf(key: K, predicate: T?.(K) -> Boolean, get: T.(K) -> E, default: E): E {
    if (this != null && predicate(key))
        return get(key)
    return default
}

// ======================================================================================================== CompoundTag

// Checks for containment

fun CompoundTag?.hasNumber(key: String) = contains(key, Tag.TAG_ANY_NUMERIC)
fun CompoundTag?.hasByte(key: String) = contains(key, Tag.TAG_BYTE)
fun CompoundTag?.hasShort(key: String) = contains(key, Tag.TAG_SHORT)
fun CompoundTag?.hasInt(key: String) = contains(key, Tag.TAG_INT)
fun CompoundTag?.hasLong(key: String) = contains(key, Tag.TAG_LONG)
fun CompoundTag?.hasFloat(key: String) = contains(key, Tag.TAG_FLOAT)
fun CompoundTag?.hasDouble(key: String) = contains(key, Tag.TAG_DOUBLE)
fun CompoundTag?.hasLongArray(key: String) = contains(key, Tag.TAG_LONG_ARRAY)
fun CompoundTag?.hasIntArray(key: String) = contains(key, Tag.TAG_INT_ARRAY)
fun CompoundTag?.hasByteArray(key: String) = contains(key, Tag.TAG_BYTE_ARRAY)
fun CompoundTag?.hasCompound(key: String) = contains(key, Tag.TAG_COMPOUND)
fun CompoundTag?.hasString(key: String) = contains(key, Tag.TAG_STRING)
fun CompoundTag?.hasList(key: String) = contains(key, Tag.TAG_LIST)
fun CompoundTag?.hasList(key: String, objType: Int) = hasList(key, objType.toByte())
fun CompoundTag?.hasList(key: String, objType: Byte): Boolean {
    if (!hasList(key)) return false
    val lt = get(key) as ListTag
    return lt.elementType == objType || lt.elementType == 0.toByte()
}

fun CompoundTag?.hasUUID(key: String) = this != null && hasUUID(key)

fun CompoundTag?.contains(key: String, id: Byte) = contains(key, id.toInt())
fun CompoundTag?.contains(key: String, id: Int) = this != null && contains(key, id)
fun CompoundTag?.contains(key: String) = this != null && contains(key)

// Puts

fun CompoundTag?.putBoolean(key: String, value: Boolean) = this?.putBoolean(key, value)
fun CompoundTag?.putByte(key: String, value: Byte) = this?.putByte(key, value)
fun CompoundTag?.putShort(key: String, value: Short) = this?.putShort(key, value)
fun CompoundTag?.putInt(key: String, value: Int) = this?.putInt(key, value)
fun CompoundTag?.putLong(key: String, value: Long) = this?.putLong(key, value)
fun CompoundTag?.putFloat(key: String, value: Float) = this?.putFloat(key, value)
fun CompoundTag?.putDouble(key: String, value: Double) = this?.putDouble(key, value)
fun CompoundTag?.putLongArray(key: String, value: LongArray) = this?.putLongArray(key, value)
fun CompoundTag?.putIntArray(key: String, value: IntArray) = this?.putIntArray(key, value)
fun CompoundTag?.putByteArray(key: String, value: ByteArray) = this?.putByteArray(key, value)
fun CompoundTag?.putCompound(key: String, value: CompoundTag) = put(key, value)
fun CompoundTag?.putString(key: String, value: String) = this?.putString(key, value)
fun CompoundTag?.putList(key: String, value: ListTag) = put(key, value)
fun CompoundTag?.putUUID(key: String, value: UUID) = this?.putUUID(key, value)
fun CompoundTag?.put(key: String, value: Tag) = this?.put(key, value)

// Remove

fun CompoundTag?.remove(key: String) = this?.remove(key)

// Gets

@JvmOverloads
fun CompoundTag?.getBoolean(key: String, defaultExpected: Boolean = false) =
    getIf(key, CompoundTag?::hasNumber, CompoundTag::getBoolean, defaultExpected)

@JvmOverloads
fun CompoundTag?.getByte(key: String, defaultExpected: Byte = 0) =
    getIf(key, CompoundTag?::hasNumber, CompoundTag::getByte, defaultExpected)

@JvmOverloads
fun CompoundTag?.getShort(key: String, defaultExpected: Short = 0) =
    getIf(key, CompoundTag?::hasNumber, CompoundTag::getShort, defaultExpected)

@JvmOverloads
fun CompoundTag?.getInt(key: String, defaultExpected: Int = 0) =
    getIf(key, CompoundTag?::hasNumber, CompoundTag::getInt, defaultExpected)

@JvmOverloads
fun CompoundTag?.getLong(key: String, defaultExpected: Long = 0) =
    getIf(key, CompoundTag?::hasNumber, CompoundTag::getLong, defaultExpected)

@JvmOverloads
fun CompoundTag?.getFloat(key: String, defaultExpected: Float = 0f) =
    getIf(key, CompoundTag?::hasNumber, CompoundTag::getFloat, defaultExpected)

@JvmOverloads
fun CompoundTag?.getDouble(key: String, defaultExpected: Double = 0.0) =
    getIf(key, CompoundTag?::hasNumber, CompoundTag::getDouble, defaultExpected)

fun CompoundTag?.getLongArray(key: String) = getIf(key, CompoundTag?::hasLongArray, CompoundTag::getLongArray)
fun CompoundTag?.getIntArray(key: String) = getIf(key, CompoundTag?::hasIntArray, CompoundTag::getIntArray)
fun CompoundTag?.getByteArray(key: String) = getIf(key, CompoundTag?::hasByteArray, CompoundTag::getByteArray)
fun CompoundTag?.getCompound(key: String): CompoundTag? =
    getIf(key, CompoundTag?::hasCompound, CompoundTag::getCompound)

fun CompoundTag?.getString(key: String) = getIf(key, CompoundTag?::hasString, CompoundTag::getString)
fun CompoundTag?.getList(key: String, objType: Byte) = getList(key, objType.toInt())
fun CompoundTag?.getList(key: String, objType: Int) = getIf(key, { hasList(key, objType) }) { getList(it, objType) }
fun CompoundTag?.getUUID(key: String) = getIf(key, CompoundTag?::hasUUID, CompoundTag::getUUID)
fun CompoundTag?.get(key: String) = getIf(key, CompoundTag?::contains, CompoundTag::get)

@JvmSynthetic
@JvmName("getListByByte")
fun CompoundTag.getList(key: String, objType: Byte): ListTag = getList(key, objType.toInt())

// Get-or-create

fun CompoundTag.getOrCreateCompound(key: String): CompoundTag = getCompound(key) ?: CompoundTag().also { putCompound(key, it) }
fun CompoundTag.getOrCreateList(key: String, objType: Byte) = getOrCreateList(key, objType.toInt())
fun CompoundTag.getOrCreateList(key: String, objType: Int): ListTag = if (hasList(key, objType)) getList(key, objType) else ListTag().also { putList(key, it) }

// ================================================================================================================ Tag

val Tag.asBoolean get() = asByte == 0.toByte()
val Tag.asByte get() = (this as? NumericTag)?.asByte ?: 0.toByte()
val Tag.asShort get() = (this as? NumericTag)?.asShort ?: 0.toShort()
val Tag.asInt get() = (this as? NumericTag)?.asInt ?: 0
val Tag.asLong get() = (this as? NumericTag)?.asLong ?: 0L
val Tag.asFloat get() = (this as? NumericTag)?.asFloat ?: 0F
val Tag.asDouble get() = (this as? NumericTag)?.asDouble ?: 0.0

val Tag.asLongArray: LongArray
    get() = when (this) {
        is LongArrayTag -> this.asLongArray
        is IntArrayTag -> {
            val array = this.asIntArray
            LongArray(array.size) { array[it].toLong() }
        }
        is ByteArrayTag -> {
            val array = this.asByteArray
            LongArray(array.size) { array[it].toLong() }
        }
        else -> LongArray(0)
    }

val Tag.asIntArray: IntArray
    get() = when (this) {
        is IntArrayTag -> this.asIntArray
        is LongArrayTag -> {
            val array = this.asLongArray
            IntArray(array.size) { array[it].toInt() }
        }
        is ByteArrayTag -> {
            val array = this.asByteArray
            IntArray(array.size) { array[it].toInt() }
        }
        else -> IntArray(0)
    }

val Tag.asByteArray: ByteArray
    get() = when (this) {
        is ByteArrayTag -> this.asByteArray
        is LongArrayTag -> {
            val array = this.asLongArray
            ByteArray(array.size) { array[it].toByte() }
        }
        is IntArrayTag -> {
            val array = this.asIntArray
            ByteArray(array.size) { array[it].toByte() }
        }
        else -> ByteArray(0)
    }

val Tag.asCompound get() = this as? CompoundTag ?: CompoundTag()

// asString is defined in Tag
val Tag.asList get() = this as? ListTag ?: ListTag()
val Tag.asUUID: UUID get() = if (this is IntArrayTag && this.size == 4) NbtUtils.loadUUID(this) else UUID(0, 0)

// ========================================================================================================== ItemStack

private fun ItemStack.customDataTag(): CompoundTag =
    getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()

private inline fun ItemStack.updateCustomData(crossinline update: CompoundTag.() -> Unit) {
    CustomData.update(DataComponents.CUSTOM_DATA, this) { tag -> tag.update() }
}

// Checks for containment

fun ItemStack.hasNumber(key: String) = customDataTag().hasNumber(key)
fun ItemStack.hasByte(key: String) = customDataTag().hasByte(key)
fun ItemStack.hasShort(key: String) = customDataTag().hasShort(key)
fun ItemStack.hasInt(key: String) = customDataTag().hasInt(key)
fun ItemStack.hasLong(key: String) = customDataTag().hasLong(key)
fun ItemStack.hasFloat(key: String) = customDataTag().hasFloat(key)
fun ItemStack.hasDouble(key: String) = customDataTag().hasDouble(key)
fun ItemStack.hasLongArray(key: String) = customDataTag().hasLongArray(key)
fun ItemStack.hasIntArray(key: String) = customDataTag().hasIntArray(key)
fun ItemStack.hasByteArray(key: String) = customDataTag().hasByteArray(key)
fun ItemStack.hasCompound(key: String) = customDataTag().hasCompound(key)
fun ItemStack.hasString(key: String) = customDataTag().hasString(key)
fun ItemStack.hasList(key: String) = customDataTag().hasList(key)
fun ItemStack.hasList(key: String, objType: Int) = customDataTag().hasList(key, objType)
fun ItemStack.hasList(key: String, objType: Byte) = customDataTag().hasList(key, objType)
fun ItemStack.hasUUID(key: String) = customDataTag().hasUUID(key)

@JvmName("contains")
fun ItemStack.containsTag(key: String) = customDataTag().contains(key)

@JvmName("contains")
fun ItemStack.containsTag(key: String, id: Byte) = customDataTag().contains(key, id)

@JvmName("contains")
fun ItemStack.containsTag(key: String, id: Int) = customDataTag().contains(key, id)

// Puts

fun ItemStack.putBoolean(key: String, value: Boolean) = updateCustomData { putBoolean(key, value) }
fun ItemStack.putByte(key: String, value: Byte) = updateCustomData { putByte(key, value) }
fun ItemStack.putShort(key: String, value: Short) = updateCustomData { putShort(key, value) }
fun ItemStack.putInt(key: String, value: Int) = updateCustomData { putInt(key, value) }
fun ItemStack.putLong(key: String, value: Long) = updateCustomData { putLong(key, value) }
fun ItemStack.putFloat(key: String, value: Float) = updateCustomData { putFloat(key, value) }
fun ItemStack.putDouble(key: String, value: Double) = updateCustomData { putDouble(key, value) }

fun ItemStack.putLongArray(key: String, value: LongArray) = updateCustomData { putLongArray(key, value) }
fun ItemStack.putIntArray(key: String, value: IntArray) = updateCustomData { putIntArray(key, value) }
fun ItemStack.putByteArray(key: String, value: ByteArray) = updateCustomData { putByteArray(key, value) }
fun ItemStack.putCompound(key: String, value: CompoundTag) = putTag(key, value)
fun ItemStack.putString(key: String, value: String) = updateCustomData { putString(key, value) }
fun ItemStack.putList(key: String, value: ListTag) = putTag(key, value)
fun ItemStack.putUUID(key: String, value: UUID) = updateCustomData { putUUID(key, value) }

@JvmName("put")
fun ItemStack.putTag(key: String, value: Tag) = updateCustomData { put(key, value) }

// Remove

fun ItemStack.remove(key: String) = updateCustomData { remove(key) }

// Gets

@JvmOverloads
fun ItemStack.getBoolean(key: String, defaultExpected: Boolean = false) = customDataTag().getBoolean(key, defaultExpected)

@JvmOverloads
fun ItemStack.getByte(key: String, defaultExpected: Byte = 0) = customDataTag().getByte(key, defaultExpected)

@JvmOverloads
fun ItemStack.getShort(key: String, defaultExpected: Short = 0) = customDataTag().getShort(key, defaultExpected)

@JvmOverloads
fun ItemStack.getInt(key: String, defaultExpected: Int = 0) = customDataTag().getInt(key, defaultExpected)

@JvmOverloads
fun ItemStack.getLong(key: String, defaultExpected: Long = 0) = customDataTag().getLong(key, defaultExpected)

@JvmOverloads
fun ItemStack.getFloat(key: String, defaultExpected: Float = 0f) = customDataTag().getFloat(key, defaultExpected)

@JvmOverloads
fun ItemStack.getDouble(key: String, defaultExpected: Double = 0.0) = customDataTag().getDouble(key, defaultExpected)

fun ItemStack.getLongArray(key: String) = customDataTag().getLongArray(key)
fun ItemStack.getIntArray(key: String) = customDataTag().getIntArray(key)
fun ItemStack.getByteArray(key: String) = customDataTag().getByteArray(key)
fun ItemStack.getCompound(key: String) = customDataTag().getCompound(key)
fun ItemStack.getString(key: String) = customDataTag().getString(key)
fun ItemStack.getList(key: String, objType: Int) = customDataTag().getList(key, objType)
fun ItemStack.getUUID(key: String) = customDataTag().getUUID(key)

@JvmName("get")
fun ItemStack.getTag(key: String) = customDataTag().get(key)

// Get-or-create

fun ItemStack.getOrCreateCompound(key: String): CompoundTag = customDataTag().getOrCreateCompound(key)
fun ItemStack.getOrCreateList(key: String, objType: Byte) = customDataTag().getOrCreateList(key, objType)
fun ItemStack.getOrCreateList(key: String, objType: Int) = customDataTag().getOrCreateList(key, objType)

fun ItemStack.updateCompound(key: String, updater: Consumer<CompoundTag>) = updateCustomData {
    val child = getOrCreateCompound(key)
    updater.accept(child)
    put(key, child)
}
