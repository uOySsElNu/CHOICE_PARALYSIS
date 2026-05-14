# Choice Paralysis / 选择困难症助手

[English](#english) | [中文](#中文)

---

## English

### Overview

Choice Paralysis is an open-source Android app designed for people who struggle with making decisions. Whether you're choosing where to eat, what to watch, or any daily dilemma, this app offers multiple fun and interactive ways to help you decide.

### Features

#### 🎡 Spin Wheel
- Canvas-drawn colorful wheel with smooth rotation animation
- Customize up to 10 options per wheel
- Real-time option editing (add, remove, modify)
- Satisfying deceleration animation with result highlight
- Save and load option group presets

#### 🪙 Coin Flip
- 3D coin flip animation with silver coin body and raised rim
- Custom heads/tails images with circular crop tool
- Save image pair presets for quick switching
- Diagonal highlight sweep after landing
- Default Tom & Jerry coin images

#### 🎲 Dice Roll
- Isometric 3D cube with proper face adjacency
- Realistic bounce and tumble animation
- Three visible faces with correct dot patterns
- Throttled face tumbling during spin
- Pop bounce effect on result

#### 👍 Yes / No
- Weighted random decisions (Yes 45% / No 45% / Maybe 10%)
- Optional question input for context
- Large emoji-based result display

#### 📋 Custom Lists
- Create and save option lists for repeated use
- Quick access from spin wheel screen
- Persistent storage via DataStore

#### 📜 History
- Automatic recording of all decisions (up to 100 entries)
- Shows decision method, options considered, and result
- Clear all or delete individual records

### Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.2.10 |
| UI Framework | Jetpack Compose |
| Design System | Material 3 with Dynamic Color |
| Navigation | Navigation Compose + NavigationSuiteScaffold |
| State Management | ViewModel + StateFlow |
| Persistence | DataStore Preferences + kotlinx-serialization |
| Image Loading | Coil 3.2.0 |
| Architecture | MVVM |
| Min SDK | 34 (Android 14) |

### Installation

Download the latest APK from [Releases](https://github.com/uOySsElNu/CHOICE_PARALYSIS/releases) and install on your Android device.

Or build from source:

```bash
git clone https://github.com/uOySsElNu/CHOICE_PARALYSIS.git
```

Open in Android Studio and run on device or emulator (API 34+).

### Permissions

- None required (all data stored locally)

---

## 中文

### 应用简介

选择困难症助手是一款开源的 Android 应用，专为"选择困难症患者"设计。无论你是纠结吃什么、看什么，还是面临任何日常抉择，这款应用都能通过多种有趣的方式帮你做出决定。

### 功能特色

#### 🎡 转盘决策
- Canvas 绘制的彩色转盘，旋转动画流畅
- 支持自定义最多 10 个选项
- 实时编辑选项（添加、删除、修改）
- 减速动画效果，结果高亮显示
- 支持保存和加载选项组合预设

#### 🪙 抛硬币
- 3D 硬币翻转动画，银色硬币主体带凸起边缘
- 自定义正反面图片，支持圆形裁切
- 保存图片组合预设，快速切换
- 落地后对角线高光扫过效果
- 默认 Tom & Jerry 硬币图片

#### 🎲 掷骰子
- 等轴测 3D 立方体，正确的面邻接关系
- 真实的弹跳翻滚动画
- 三个可见面显示正确的点数图案
- 旋转过程中节流的点数切换
- 结果弹出效果

#### 👍 Yes / No 决策
- 加权随机决策（是 45% / 否 45% / 也许 10%）
- 可输入问题作为决策背景
- 大号 Emoji 结果展示

#### 📋 自定义选项列表
- 创建并保存常用选项列表，方便重复使用
- 从转盘界面快速加载已保存的列表
- 使用 DataStore 持久化存储

#### 📜 历史记录
- 自动记录所有决策结果（最多 100 条）
- 显示决策方式、考虑的选项和最终结果
- 支持清空全部或删除单条记录

### 技术栈

| 组件 | 技术 |
|------|------|
| 开发语言 | Kotlin 2.2.10 |
| UI 框架 | Jetpack Compose |
| 设计系统 | Material 3 + 动态取色 |
| 导航 | Navigation Compose + NavigationSuiteScaffold |
| 状态管理 | ViewModel + StateFlow |
| 持久化 | DataStore Preferences + kotlinx-serialization |
| 图片加载 | Coil 3.2.0 |
| 架构模式 | MVVM |
| 最低 SDK | 34 (Android 14) |

### 安装方法

从 [Releases](https://github.com/uOySsElNu/CHOICE_PARALYSIS/releases) 页面下载最新 APK 安装到 Android 设备。

或从源码构建：

```bash
git clone https://github.com/uOySsElNu/CHOICE_PARALYSIS.git
```

使用 Android Studio 打开项目，在设备或模拟器上运行（API 34+）。

### 权限说明

- 无需任何权限（所有数据本地存储）

---

## License / 许可证

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

本项目采用 MIT 许可证 - 详情请查看 [LICENSE](LICENSE) 文件。

---

## Contributing / 贡献

Contributions are welcome! Feel free to submit issues and pull requests.

欢迎贡献代码！请随时提交 Issue 和 Pull Request。
