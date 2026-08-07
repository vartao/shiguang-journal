<template>
  <div class="film-strip" :class="{ vertical }">
    <div class="film-strip__track">
      <div v-for="i in frameCount" :key="i" class="film-strip__frame">
        <div class="film-strip__sprocket top"></div>
        <div class="film-strip__cell">
          <slot :name="'frame-' + i">
            <div class="film-strip__cell-inner"></div>
          </slot>
        </div>
        <div class="film-strip__sprocket bottom"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  frameCount: { type: Number, default: 8 },
  vertical: { type: Boolean, default: false }
})
</script>

<style scoped>
.film-strip {
  overflow: hidden;
  position: relative;
}

.film-strip__track {
  display: flex;
  gap: 0;
  background: var(--film-black);
  padding: 8px 0;
  border-radius: 3px;
  box-shadow:
    inset 0 2px 4px rgba(0, 0, 0, 0.5),
    0 2px 8px rgba(0, 0, 0, 0.2);
}

.film-strip.vertical .film-strip__track {
  flex-direction: column;
  padding: 0 8px;
}

.film-strip__frame {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 0 6px;
  flex-shrink: 0;
}

.film-strip.vertical .film-strip__frame {
  flex-direction: row;
  padding: 6px 0;
}

/* 齿孔 */
.film-strip__sprocket {
  width: 10px;
  height: 7px;
  background: var(--cream);
  border-radius: 2px;
  opacity: 0.7;
  flex-shrink: 0;
}

.film-strip.vertical .film-strip__sprocket {
  width: 7px;
  height: 10px;
}

/* 画面格子 */
.film-strip__cell {
  width: 64px;
  height: 44px;
  overflow: hidden;
  border-radius: 1px;
  border: 1px solid rgba(var(--cream-static-rgb), 0.15);
}

.film-strip.vertical .film-strip__cell {
  width: 44px;
  height: 64px;
}

.film-strip__cell-inner {
  width: 100%;
  height: 100%;
  background: linear-gradient(145deg, rgba(var(--brown-rgb), 0.4), rgba(45, 36, 32, 0.6));
  filter: sepia(0.4) brightness(0.7);
}

/* 装饰用途时的半透明效果 */
.film-strip--decorative {
  opacity: 0.4;
  pointer-events: none;
}
</style>
